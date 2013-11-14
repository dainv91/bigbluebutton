package org.bigbluebutton

import com.typesafe.config.ConfigFactory
import scala.util.Try

trait SystemConfiguration {

  val config = ConfigFactory.load()

  lazy val serviceHost = Try(config.getString("service.host")).getOrElse("127.0.0.1")

  lazy val servicePort = Try(config.getInt("service.port")).getOrElse(8080)

}