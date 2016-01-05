package ActivePassivePattern

import akka.actor._
import akka.cluster.Cluster
import play.api.libs.json.JsValue

import scala.collection.immutable.TreeMap
import scala.collection.mutable
import scala.concurrent.duration._

/**
 * Active actor
 */
class Active(localReplica: ActorRef, replicationFactor: Int, maxQueueSize: Int)
    extends Actor with Stash with ActorLogging {

  private var theStore: Map[String, JsValue] = _
  private var seqNr: Iterator[Int] = _
  private val MaxOutStanding = maxQueueSize / 2
  private val toReplicate = mutable.Queue.empty[Replicate]
  private var replicating = TreeMap.empty[Int, (Replicate, Int)]

  log.info("taking over from local replica")
  localReplica ! TakeOver(self)

  import Active._
  import context._

  val timer = system.scheduler.schedule(1.second, 1.second, self, Tick)

  override def postStop(): Unit = {
    timer.cancel()
  }

  override def receive: Receive = {
    case InitialState(m ,s) =>
      log.info("took over at sequence {}", s)
      theStore = m
      seqNr = Iterator from s
      context.become(running)
      unstashAll()
    case _ => stash()
  }

  val running: Receive = {
    case p @ Put(key, value, replyTo) =>
      if (toReplicate.size < MaxOutStanding) {
        toReplicate.enqueue(Replicate(seqNr.next, key, value, replyTo))
        replicate()
      } else {
        replyTo ! PutRejected(key, value)
      }
    case Get(key, replyTo) =>
      replyTo ! GetResult(key, theStore get key)
    case Tick =>
      replicating.valuesIterator foreach {
        case (replicate, count) => disseminate(replicate)
      }
    case Replicated(confirm) =>
      replicating.get(confirm) match {
        case None =>
        case Some((rep, 1)) =>
          replicating -= confirm
          theStore += rep.key -> rep.value
          rep.replyTo ! PutConfirmed(rep.key, rep.value)
        case Some((rep, n)) =>
          replicating += confirm -> (rep, n - 1)
      }
      replicate()
  }

  private def replicate(): Unit = {
    if (replicating.size < MaxOutStanding && toReplicate.nonEmpty) {
      val r = toReplicate.dequeue()
      replicating += r.seq -> (r, replicationFactor)
      disseminate(r)
    }
  }

  private def disseminate(r: Replicate): Unit = {
    val req = r.copy(replyTo = self)
    def replicaOn(addr: Address): ActorSelection =
    actorSelection(localReplica.path.toStringWithAddress(addr))
    val members = Cluster(system).state.members
    members.foreach(m => replicaOn(m.address) ! req)
  }

}

object Active {
  case object Tick
}