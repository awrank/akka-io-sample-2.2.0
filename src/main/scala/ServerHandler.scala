import akka.actor.{Actor,ActorRef,ActorSystem,Props,ActorLogging}
import akka.io.{Tcp, TcpPipelineHandler, PipelineContext}
import akka.util.ByteString
import java.net.InetSocketAddress

/**
 * サーバ側のセッション処理
 */
class ServerHandler(init: TcpPipelineHandler.Init[_ <: PipelineContext,Command,Event]) extends Actor with ActorLogging {

  /* 起動しますた！(｀・ω・´) */
  override def preStart {
    log.info("Session Start!")
  }

  /* 具体的な処理 */
  def receive = {

    /* クライアントからのメッセージはinit.Eventをラッパーにして飛んでくる */
    case init.Event(evt) => evt match {
      case Ping => {
        log.info("ping received!")
        sender ! init.Command(Pong) // サーバからの応答の送信はinit.Commandでラップする
      }
      /*
       * TcpPipelineHandler.Manager(cmd) を送信すると全てのPipelineStageにメッセージを送信できる。
       * パイプラインの最下部に位置するTcpReadWriteAdapterはmanagementPortでこれを受信し、そのままmanagerに送信する。
       * これによって、コネクションを管理するアクターにCloseを送信できる。
       */
      case Quit => {
        sender ! init.Command(Quitted)
        sender ! TcpPipelineHandler.Management(Tcp.Close)
      }

      /* 他のイベント */
      case SomeEvent(mes) => {
        log.warning("Unknown " + mes + " received!")
        sender ! init.Command(SomeCommand(mes))
      }
    }

    /* コマンド送信ミスの場合 */
    case Tcp.CommandFailed(cmd) => {
      log.error(cmd.toString + " failed")
      context stop self
    }

    /* Tcp.Event は直接ハンドラに伝えられる。(ここではCloseの応答を待つ) */
    case Tcp.Closed => context stop self
  }
}

/** クライアントからのイベント */
trait Event
case object Ping extends Event
case object Quit extends Event
case class SomeEvent(mes: String) extends Event

/** クライアントへのコマンド */
trait Command
case object Pong extends Command
case object Quitted extends Command
case class SomeCommand(mes: String) extends Command
