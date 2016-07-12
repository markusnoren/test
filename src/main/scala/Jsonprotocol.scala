package org.smartjava

import spray.json.DefaultJsonProtocol


object MyJsonProtocol extends DefaultJsonProtocol
{
  implicit val personFormat = jsonFormat3(Person)
}


case class Person(firstName: String, lastName: String, phoneNumber: String)