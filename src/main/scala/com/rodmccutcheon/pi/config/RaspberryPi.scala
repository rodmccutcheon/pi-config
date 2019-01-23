package com.rodmccutcheon.pi.config

import java.io._
import java.util.Properties

import com.jcraft.jsch.{ChannelExec, ChannelShell, JSch}
import com.typesafe.scalalogging.LazyLogging
import org.apache.commons.io.IOUtils

import scala.collection.mutable
import scala.io.Source

class RaspberryPi(address: String) extends Gateway with LazyLogging {
  val sshParams = SshParams.parse(address)

  val jsch = new JSch
  val session = jsch.getSession(sshParams.username, sshParams.hostname)
  session.setPassword(sshParams.password)

  val config = new Properties
  config.put("StrictHostKeyChecking", "no")
  session.setConfig(config)
  session.connect(5000)

  override val macAddress: String = runCommand("python ")

  private def runCommand(cmd: String, timeout: Int = 0): String = {
    logger.debug(s"Sending the following command: $cmd")
    val channel = session.openChannel("exec").asInstanceOf[ChannelExec]
    channel.setCommand(cmd)
    val reader = new BufferedReader(new InputStreamReader(channel.getInputStream))
    channel.connect()
    val line = reader.readLine()
    Thread.sleep(timeout)
    channel.disconnect()
    line
  }

  private def runSequentialCommands(cmds: Array[(String,String)]) = {
    val cmd = cmds.foldLeft("")((l, r) => (l + (if (l == "") "" else "&&") + r._2 + "&&echo \"***PROCESS_" + r._1 +
      "_DONE***\"")) + "&&exit"
    val cmdConfirmationPattern = """^\*\*\*PROCESS_([\w\d\s]+)_DONE\*\*\*$""".r
    val channel = session.openChannel("shell").asInstanceOf[ChannelShell]
    val printStream = new PrintStream(channel.getOutputStream(), true)
    val streamReader = new InputStreamReader(channel.getInputStream())
    val bufferedReader = new BufferedReader(streamReader)
    channel.connect()
    printStream.println(cmd)
    printStream.close()

    def checkInput = {
      if (bufferedReader.ready()) {
        val line = bufferedReader.readLine()
        line match {
          case cmdConfirmationPattern(description) => logger.debug(s"Finished executing on gateway: $description.")
          case _ => logger.debug(line)
        }
      }
    }

    while (!channel.isClosed() && channel.getExitStatus() == -1) {
      Thread.sleep(100)
      checkInput
    }

    while (bufferedReader.ready()) {
      checkInput
    }

    channel.disconnect()
  }

  private def writeFile(filename: String, bytes: Array[Byte], baseDirectory: String = "/home/admin") = {
    val channel = session.openChannel("exec").asInstanceOf[ChannelExec]
    channel.setCommand(s"scp -t ${baseDirectory}/$filename")

    val in = channel.getInputStream
    val out = channel.getOutputStream

    channel.connect()

    out.write(s"C0644 ${bytes.length} $filename\n".getBytes)
    out.flush()

    throwOnError(in)

    IOUtils.copy(new ByteArrayInputStream(bytes), out)
    out.write(Array(0x0.toByte))
    out.flush()
    out.close()

    throwOnError(in)

    channel.disconnect()
  }

  private def throwOnError(in: InputStream) = {
    val b = in.read
    if (b != 0) throw new RuntimeException(s"Got error code $b during scp!")
  }

  def close() = session.disconnect()

  override def toString = s"RaspberryPi($macAddress)"
}

object RaspberryPi {
  private val MacAddressOctetPattern = "[0-9a-f]{2}".r
  private val PythonDirectory = "/usr/lib/python2.7/site-packages"
  private val WirepasDirectory = PythonDirectory + "/wirepas"
  private val ORTConfigDirectory = PythonDirectory + "/ort_config"
}

case class SshParams(username: String, password: String, hostname: String) {}

object SshParams {
  def parse(address: String): SshParams = {
    val fullUrl = """([^:]*):([^@]*)@(.*)""".r

    address match {
      case fullUrl(username, password, hostname) => SshParams(username, password, hostname)
      case _ => throw new RuntimeException(s"Can't parse $address as username:password@host")
    }
  }
}
