package nioUDP

import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.parallel.*

import java.net.{InetSocketAddress, StandardProtocolFamily, StandardSocketOptions}
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.nio.charset.{Charset, CharsetDecoder}
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import scala.concurrent.duration.*

object NioUdp extends IOApp {
  private val N_BYTES: Int = 65507
  def server(): Unit =
    val port: Int = 5555
    val ip: String = "127.0.0.1"
    Try {
      val datagramChannel = DatagramChannel.open(StandardProtocolFamily.INET) // Use IPv4
      if (datagramChannel.isOpen) {
        println("[SERVER] Successfully opened UDP server")

        // Prepare socket
        datagramChannel.setOption(StandardSocketOptions.SO_RCVBUF, 4 * 1024)
        datagramChannel.setOption(StandardSocketOptions.SO_SNDBUF, 4 * 1024)

        datagramChannel.bind(new InetSocketAddress(ip, port))
        println(s"s[SERVER] UDP server bound to ${datagramChannel.getLocalAddress}")

        // Send data packets
        val content = ByteBuffer.allocate(N_BYTES)
        while (true) {
          val clientAddress = datagramChannel.receive(content)
          content.flip()

          println(s"[SERVER] I've received ${content.limit()} bytes from ${clientAddress.toString}. Sending them back...")

          datagramChannel.send(content, clientAddress)
          content.clear()
        }
      }
      else {
        println("[SERVER] Unable to open channel")
      }
    } match {
      case Failure(ex) =>
        println(s"[CLIENT] ${ex.getMessage}")
      case Success(_) =>
        println("[CLIENT] Everything works fine")
    }

  def client(): Unit =
    val serverPort: Int = 5555
    val serverIp: String = "127.0.0.1"

    val charSet: Charset = Charset.defaultCharset()
    val decoder: CharsetDecoder = charSet.newDecoder()

    val echoText: ByteBuffer = ByteBuffer.wrap("[MESSAGE] I was sent back from the server".getBytes())
    val content: ByteBuffer = ByteBuffer.allocate(N_BYTES)

    Try {
      val datagramChannel = DatagramChannel.open(StandardProtocolFamily.INET)
      if (datagramChannel.isOpen) {
        datagramChannel.setOption(StandardSocketOptions.SO_RCVBUF, 4 * 1024)
        datagramChannel.setOption(StandardSocketOptions.SO_SNDBUF, 4 * 1024)

        // Send to server
        val sent = datagramChannel.send(echoText, new InetSocketAddress(serverIp, serverPort))
        println(s"[CLIENT] I have successfully sent $sent bytes to the echo server!")

        // Get echo back
        datagramChannel.receive(content)
        content.flip()
        val charBuffer = decoder.decode(content)
        println(charBuffer.toString)
        content.clear()
        datagramChannel.close()
      } else {
        println("[CLIENT] The channel cannot be opened")
      }
    } match {
      case Failure(ex) =>
        println(s"[CLIENT] ${ex.getMessage}")
      case Success(_) =>
        println("[CLIENT] Everything works fine")
    }

  override def run(args: List[String]): IO[ExitCode] =
    (IO(server()), IO.sleep(500.millis).flatMap(_ => IO(client())))
      .parMapN((s, c) => ())
      .as(ExitCode.Success)
}
