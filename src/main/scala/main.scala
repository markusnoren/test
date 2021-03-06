package org.smartjava;

//Cassandra spark spray test
import akka.actor.{ActorSystem, Props}
import akka.actor.{ActorLogging, Actor}
import akka.io.IO
import spray.can.Http
import akka.pattern.ask
import akka.util.Timeout
import spray.can.Http
import scala.concurrent.duration._
import spray.httpx.SprayJsonSupport._
import spray.json.DefaultJsonProtocol
import spray.routing._


import org.apache.spark.SparkContext._
import org.apache.spark.{SparkContext, SparkConf}
import com.datastax.spark.connector._
import com.datastax.spark.connector.cql.CassandraConnector

import org.apache.spark.rdd.EmptyRDD

import org.apache.hadoop.io.compress.GzipCodec
import spray.httpx.encoding.{Gzip, Deflate}

object Boot extends App
{
  //Create actor system
  implicit val system = ActorSystem("CassandraAccessSystem")
  //Create actor
  val accessActor = system.actorOf(Props[CassandraAccessActor],"cassandraAccessActor")
  //A timeout is needed for the ask method next
  implicit val timeout = Timeout(5.seconds)

  //start a httpserver with apiActor as its handler
  IO(Http) ? Http.Bind(accessActor, interface = "127.0.0.1", port = 9160)

  println("Hit any key to exit.")
  val result = readLine()
  system.shutdown()
}

case class Contact(id: Int, firstname: String, lastname: String, email: String)
object ContactProtocol extends DefaultJsonProtocol
{
  implicit val ContactFormat = jsonFormat4(Contact)
}
import ContactProtocol._


class CassandraAccessActor extends Actor with HttpService with ActorLogging
{
  //Create a connection to cassandra
  val conf = new SparkConf(true).set("spark.cassandra.connection.host", "127.0.0.1:9160").setAppName("CassandraAccessor").setMaster("local")
  val sc = new SparkContext(conf)

  //val keyspace = "contacts"
  //val table = "my_contacts"

  def actorRefFactory = context

  def receive = runRoute(apiRoute)

  val apiRoute: Route =
  {
    get
    {
      path("test")
      {
        complete
        {
          "n'yelloh"
        }
      }~
      path(Segment / Segment / "show" / "all")
      {
        (keyspace,table)=>
          complete
          {
            val rdd = sc.cassandraTable[Contact](keyspace, table)

            rdd.toArray
          }
      }~
      path(Segment / Segment / "show" / Segment)
      {
        (keyspace, table, fname) =>
          complete
          {
            if(fname.contains("*"))
            {
              var rdd = sc.cassandraTable[Contact](keyspace, table).select("id", "firstname", "lastname", "email").where("id = ?", 1).where("firstname > ?", fname.replaceAll("[*]", "a")).where("firstname < ?", fname.replaceAll("[*]", "z"))
              rdd.toArray
            }
            else
            {
              var rdd = sc.cassandraTable[Contact](keyspace, table).select("id", "firstname", "lastname", "email").where("id = ?", 1).where("firstname = ?", fname)
              rdd.toArray
            }


          }
      }~
      path(Segment / Segment / "show" / Segment / Segment)
      {
        (keyspace, table, fname, lname) =>
          complete
          {
            var rdd = sc.cassandraTable[Contact](keyspace, table).select("id","firstname","lastname","email").where("id = ?", 1)

            //Checking firstname
            if(fname.contains("*"))
            {
              rdd = rdd.where("firstname > ?", fname.replaceAll("[*]", "a")).where("firstname < ?", fname.replaceAll("[*]", "z"))
            }
            else
            {
              rdd = rdd.where("firstname = ?", fname)
            }

            //Cheking lastname
            if(lname.contains("*"))
            {
              rdd = rdd.where("lastname > ?", lname.replaceAll("[*]", "a")).where("lastname < ?", lname.replaceAll("[*]", "z"))
            }
            else
            {
              rdd = rdd.where("lastname = ?", lname)
            }


            rdd.toArray
          }
      }~
      path(Segment / Segment / "delete" / Segment / Segment)
      {
        (keyspace, table, fname, lname)=>
        complete
        {
          CassandraConnector(conf).withSessionDo
          {
            session=>
              session.execute(s"DELETE FROM $keyspace.$table WHERE id = 1 AND firstname='${fname}' AND lastname = '${lname}';")
          }
          "Deleted"
        }
      }~
      path(Segment / Segment / "export" / Segment)
      {
        (keyspace,table, filename)=>
        complete
        {
          val rdd = sc.cassandraTable[Contact](keyspace, table)

          rdd.saveAsTextFile(filename, classOf[GzipCodec])
          sc.textFile(filename)

          "Exported"
        }
      }
    }~
    post
    {
      path(Segment / Segment / "new")
      {
        (keyspace, table)=>
        handleWith
        {
          contact: Contact =>
            //created rdd dataset from parallelize
            val collection = sc.parallelize(Seq(contact))
            collection.saveToCassandra(keyspace,table, SomeColumns("id","firstname", "lastname", "email"))

            "Added"
        }
      }
    }


    /*
    path("contacts")
    {

      get //Returns contacts in cassandra
      {
        log.info("Building port route")
        complete
        {
          log.info("Executing get route")
          //Resilient Distributed Dataset
          val rdd = sc.cassandraTable[Contact](keyspace, table)

          rdd.saveAsTextFile("Export.gz", classOf[GzipCodec])
          sc.textFile("Export.gz")


          //convert the rdd dataset to an array
          rdd.toArray
        }
      } ~ post // add a new robot
      {
        log.info("Building post route")
        handleWith
        {
          contact: Contact =>
            log.info("Executing post route")
            //created rdd dataset from parallelize
            val collection = sc.parallelize(Seq(contact))
            collection.saveToCassandra(keyspace,table, SomeColumns("id","firstname", "lastname", "email"))
            //Show the added item
            contact
        }
      } ~ put //used to search
      {
        log.info("Building put route")
        handleWith
        {
          contact: Contact =>
            log.info("Executing put route")

            var rdd = sc.cassandraTable[Contact](keyspace, table).select("id","firstname","lastname","email").where("id = ?", 1)

            if(contact.firstname != "" )
            {
              if(contact.firstname.contains("*"))
              {
                var searchfront = contact.firstname.replaceAll("[*]", "a")
                val searchback  = contact.firstname.replaceAll("[*]", "z")
                rdd = rdd.where("firstname > ?", contact.firstname.replaceAll("[*]", "a")).where("firstname < ?", contact.firstname.replaceAll("[*]", "z"))
              }
              else
              {
                rdd = rdd.where("firstname = ?", s"${contact.firstname}")
              }
            }

            if(contact.lastname != "" )
            {
              if(contact.lastname.contains("*"))
              {
                var searchfront = contact.lastname.replaceAll("[*]", "a")
                val searchback  = contact.lastname.replaceAll("[*]", "z")
                rdd = rdd.where("lastname > ?", contact.lastname.replaceAll("[*]", "a")).where("lastname < ?", contact.lastname.replaceAll("[*]", "z"))
              }
              else
              {
                rdd = rdd.where("lastname = ?", s"${contact.lastname}")
              }
            }

            rdd.toArray
        }
      } ~ delete //delete
      {
        log.info("Building delete route")
        handleWith
        {
          contact: Contact =>
            log.info("Executing delete route")
            CassandraConnector(conf).withSessionDo
            {
              session=>
                session.execute(s"DELETE FROM $keyspace.$table WHERE id = 1 AND firstname='${contact.firstname}' AND lastname = '${contact.lastname}';")
            }
            contact
        }
      }
    } ~path("test")
    {
      get //Returns testkeys in cassandra
      {
        log.info("Building port route")
        complete
        {
          log.info("Executing get route")
          //Resilient Distributed Dataset
          val rdd = sc.cassandraTable[Contact](keyspace, table)

          //convert the rdd dataset to an array
          rdd.toArray
        }
      }
    }
    */
  }

}


/*
object Main extends App
{
  val host = "localhost"
  //val port = 8080
val port = 5000

  implicit val system = ActorSystem("contact-management-service")

  val api = system.actorOf(Props(new RestInterface()), "httpInterface")

  implicit val executionContext = system.dispatcher
  implicit val timeout = Timeout(10 seconds)

  IO(Http).ask(Http.Bind(listener = api, interface = host, port = port))
    .mapTo[Http.Event]
      .map
      {
        case Http.Bound(address) =>
          println(s"REST interface bound to address")
        case Http.CommandFailed(cmd) =>
          println("REST interface could not bind to" + s"$host:$port, ${cmd.failureMessage}")
        system.shutdown()
      }
}

*/




