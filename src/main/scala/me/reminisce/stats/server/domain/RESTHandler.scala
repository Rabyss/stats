package me.reminisce.stats.server.domain

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{OneForOneStrategy, _}
import me.reminisce.stats.server.domain.Domain.{ActionForbidden, Error}
import me.reminisce.stats.server.domain.RESTHandler.{WithActorRef, WithProps}
import me.reminisce.stats.server.jsonserializer.StatsFormatter
import spray.http.StatusCode
import spray.http.StatusCodes._
import spray.httpx.Json4sSupport
import spray.routing.RequestContext

import scala.concurrent.duration._

/**
  * Defines an actor handling rest messages
  */
trait RESTHandler extends Actor with Json4sSupport with ActorLogging with StatsFormatter{

  import context._

  def r: RequestContext

  def target: ActorRef

  def message: RestMessage

  setReceiveTimeout(10.seconds)
  // sends the request to an actor
  target ! message

  /**
    * Defines the way rest messages are handled (translated to a call to the
    * [[me.reminisce.stats.server.domain.RESTHandler.complete]] method)
 *
    * @return
    */
  def receive = {
    case error: Error => complete(NotFound, error)
    case tooMany: Domain.TooManyRequests => complete(TooManyRequests, tooMany)
    case v: Domain.Validation => complete(BadRequest, v)
    case ReceiveTimeout => complete(GatewayTimeout, Domain.Error("Request timeout"))
    case internalError: Domain.InternalError => complete(InternalServerError, internalError)
    case forbidden: ActionForbidden => complete(Forbidden, forbidden)
    case res: RestMessage => complete(OK, res)
    case x =>
      log.info("Per request received strange message " + x)
      complete(InternalServerError, "The request resulted in an unexpected answer.")
  }

  /**
    * Complete the request with the given message and status
 *
    * @param status status of the response
    * @param obj response content
    * @tparam T type of the content
    */
  def complete[T <: AnyRef](status: StatusCode, obj: T) = {
    r.complete(status, obj)
    stop(self)
  }

  override val supervisorStrategy =
    OneForOneStrategy() {
      case e =>
        complete(InternalServerError, Domain.Error(e.getMessage))
        Stop
    }
}

/**
  * [[me.reminisce.stats.server.domain.RESTHandler]] implementations
  */
object RESTHandler {

  case class WithActorRef(r: RequestContext, target: ActorRef, message: RestMessage) extends RESTHandler

  case class WithProps(r: RequestContext, props: Props, message: RestMessage) extends RESTHandler {
    lazy val target = context.actorOf(props)
  }

}

/**
  * Factory for [[me.reminisce.stats.server.domain.RESTHandler]]
  */
trait RESTHandlerCreator {
  this: Actor =>

  /**
    * Creates a rest handler
 *
    * @param r request context to handle
    * @param target target actor to handle the request
    * @param message request to send
    * @return reference to created rest handler
    */
  def perRequest(r: RequestContext, target: ActorRef, message: RestMessage) =
    context.actorOf(Props(new WithActorRef(r, target, message)))

  /**
    * Creates a rest handler
 *
    * @param r request context to handle
    * @param props props for the target actor to handle the request
    * @param message request to send
    * @return reference to created rest handler
    */
  def perRequest(r: RequestContext, props: Props, message: RestMessage) =
    context.actorOf(Props(new WithProps(r, props, message)))
}