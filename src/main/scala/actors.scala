package org.smartjava

import akka.actor._
import akka.util.Timeout
import spray.routing._
import spray.http._
import MediaTypes._
import spray.httpx.SprayJsonSupport._
import spray.json.DefaultJsonProtocol
import scala.concurrent.{ExecutionContext, Future}

/*
class SJServiceActor extends Actor with HttpService
{

  def actorRefFactory = context

  def receive = runRoute(aSimpleRoute ~anotherRoute)


  val aSimpleRoute =
  {
    path("path1")
    {
      get
      {
        headerValue({

          case x@HttpHeaders.`Content-Type`(value) => Some(value)
          case default => None
        })
        {
          header => header match
          {
            case ContentType(MediaType("application/vnd.type.a"),_) =>
            {
              respondWithMediaType(`application/json`)
              {
                complete
                {
                  Person("Anders", "Anderson", "1234567890")
                }
              }
            }
            case ContentType(MediaType("application/vnd.type.b"),_) =>
            {
              respondWithMediaType(`application/json`)
              {
                complete
                {
                  Person("Bob", "Bobertson", "0123456789")
                }
              }
            }
            case default =>
            {
              complete
              {
                HttpResponse(406)
              }
            }
          }
        }
      }
    }
  }

  val anotherRoute =
  {
    path("path2")
    {
      get
      {
        respondWithMediaType(`text/html`)
        {
          complete
          {
            <html>
              <body>
                <h1>Path 2</h1>
              </body>
            </html>
          }
        }
      }
    }
  }

}
*/


/*
case class Contact(id:Int, firstName: String, lastName: String, email: String)

case class ContactAdded

case class ContactAlreadyExists

case class ContactDeleted

object Contact extends DefaultJsonProtocol
{
  implicit val format = jsonFormat4(Contact.apply)
}

class RestInterface extends HttpServiceActor with RestApi
{
  def receive = runRoute(routes)
}

trait RestApi extends HttpService with ActorLogging
{actor: Actor =>

  //implicit val timeout = Timeout(10 seconds)

  var contacts = Vector[Contact]()

  def routes: Route = pathPrefix("contacts")
  {
    pathEnd
    {
      post
      {
        entity(as[Contact])
        {
          contact => requestContext =>
            val responder = createResponder(requestContext)
            createContact(contact) match
            {
              case true =>
                responder ! ContactAdded
              case _ =>
                responder ! ContactAlreadyExists
            }
        }
      }
    } ~
    path(Segment)
    {
      id => delete
      {
        requestContext =>
          val responder = createResponder(requestContext)
          //deleteContact(id)
          responder ! ContactDeleted
      }
    }
  }

  private def createContact(contact:Contact): Boolean =
  {
    val doesNotExist = !contacts.exists(_.id==contact.id)
    if(doesNotExist)
      contacts = contacts:+contact

    doesNotExist
  }

  private def deleteContact(id: Int): Unit =
  {
    contacts = contacts.filter(_.id != id)
  }

  private def createResponder(requestContext: RequestContext)=
  {
    context.actorOf(Props(new Responder(requestContext)))
  }
}

class Responder(requestContext:RequestContext) extends Actor with ActorLogging
{
  def receive =
  {
    case ContactAdded =>
      requestContext.complete(StatusCodes.Created)
      killYourself

    case ContactDeleted =>
      requestContext.complete(StatusCodes.OK)
      killYourself

    case ContactAlreadyExists =>
      requestContext.complete(StatusCodes.Conflict)
      killYourself
  }

  private def killYourself = self ! PoisonPill
}

trait MyHttpService extends HttpService// with JsonSupport
{
  implicit val execitionContext: ExecutionContext
  def completeWithLocationHeader[T](resourceId: Future[Option[T]], ifDefinedStatus: Int, ifEmptyStatus: Int) :
  Route= onSuccess(resourceId)
  {
    maybeT => maybeT match
    {
      case Some(t) => completeWithLocationHeader(ifDefinedStatus, t)
      //case None => complete(ifEmptyStatus, None)
    }
  }

  def completeWithLocationHeader[T](status: Int, resourceId: T):
  Route = requestInstance
  {
    request =>
    val location = request.uri.copy(path = request.uri.path / resourceId.toString)
    respondWithHeader(HttpHeader.Location(location))
    {
      complete(status,None)
    }
  }
}

trait ContactResource extends MyHttpService
{
  val contactService: ContactService

  def contactRoutes: Route = pathPrefix("contacts")
  {
    pathEnd
    {
      post
      {
        entity(as[Contact])
        {
          contact => completeWithLocationHeader(resourceId = contactService.createContact(contact),ifDefinedStatus = 201, ifEmptyStatus = 409)
        }
      }
    }//~

  }
}
*/