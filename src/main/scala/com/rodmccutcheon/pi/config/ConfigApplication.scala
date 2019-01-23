package com.rodmccutcheon.pi.config

import ch.qos.logback.classic.{Level, Logger}
import com.typesafe.scalalogging.StrictLogging
import org.slf4j.LoggerFactory

object ConfigApplication extends App with StrictLogging {
  val Option = """-(\w{1,3})(?:=(.*))?""".r

  val Commands = Map(
    "print-mac" -> {
      (args: List[String], gateway: Gateway) => {
        println(s"The gateway's MAC address is ${gateway.macAddress}.")
      }
    }
  )

  def usage =
    """
      | [-l=log-level] <gateway-address> <command> [<args>]
      |
      |Supported commands are:""".stripMargin + "\n" + Commands.keys.toList.sorted.map("\t" + _ + "\n").foldLeft("")((a, b) => a + b)

  val (optionArgs, executionArgs) = args.toList.span {
    case Option(_*) => true
    case _ => false
  }
  val options = optionArgs
    .map {
      case Option(optionName: String, optionValue: String) => optionName -> optionValue
      case Option(optionName: String, _) => optionName -> "true"
    }.toMap


  if (options contains "l") {
    val logger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).asInstanceOf[Logger]
    logger.setLevel(Level.valueOf(options("l")))
  }

  executionArgs match {
    case (address :: command :: args) => {
      val gateway = new RaspberryPi(address)
      try {
        logger.trace(gateway.toString)

        if (Commands contains command) {
          logger.debug(s"Running '$command'...")
          val commandFn = Commands(command)
          commandFn(args, gateway)
        }
        else {
          logger.error(s"Command $command does not exist!")
          println(usage)
        }
      } finally {
        gateway.close
      }

    }
    case _ => println(usage)
  }
}
