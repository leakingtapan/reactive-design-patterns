import akka.actor.ActorRef
import play.api.libs.json.JsValue

/**
 * Created by chengpan on 1/4/16.
 */
package object ActivePassivePattern {

  case class Put(key: String, value: JsValue, replayTo: ActorRef)
  case class PutConfirmed(key: String, value: JsValue)
  case class PutRejected(key: String, value: JsValue)
  case class Get(key: String, replayTo: ActorRef)
  case class GetResult(key: String, value: Option[JsValue])

  case class TakeOver(owner: ActorRef)
  case class InitialState(store: Map[String, JsValue], seq: Int)
  case class Replicate(seq: Int, key: String, value: JsValue, replyTo: ActorRef)
  case class Replicated(seq: Int)
}
