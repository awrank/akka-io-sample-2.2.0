import akka.actor.{Actor,ActorRef,ActorSystem,Props,ActorLogging,Terminated}
import akka.io.{IO,Tcp,TcpPipelineHandler}
import akka.util.ByteString
import java.net.InetSocketAddress

/**
 * Listenなどのサーバ開始時処理と、セッション生成のみを実行する
 */
class Server extends Actor with ActorLogging {
  import context.system
  import Tcp._

  /* コネクションマネージャ(Bound時に得られる) */
  private var manager: Option[ActorRef] = None
  private var count: Int = 0


  /* 起動時にListen開始 */
  override def preStart {
    IO(Tcp) ! Bind(self, new InetSocketAddress("localhost", 8080))
  }

  def receive = {

    /* Listen成功 : manager を保存する。 */
    case Bound(addr) => {
      log.info("Start listening on " + addr.toString)
      manager = Some(sender)
    }

    /* 接続時：セッションハンドラとパイプラインを起動してセッション開始 */
    case Connected(remote, local) => {
      log.info("Connected: " + remote.getHostName + " -> " + local.getHostName)
      count += 1

      // log っていつ使われるんだろう…？
      val init = TcpPipelineHandler.withLogger(log, ServerPipelineStage())
      val session = context.actorOf(Props(new ServerHandler(init)), s"ServerHandler-${count}")
      context.watch(session)
      val pipeline = context.actorOf(TcpPipelineHandler.props(init, sender, session), s"TcpPipelineHandler-${count}")
      context.watch(pipeline)
      sender ! Register(pipeline)
    }

    case Terminated(actor) => log.info(actor.path.name + " terminated.")

    /* 停止 */
    case Stop => manager match {
      case Some(actor) => { log.info("Stop listening") ; actor ! Unbind }
      case None => log.warning("Already stopped")
    }

    /* Listen解除した */
    case Unbound => context stop self

    /* 何らかのコマンドに失敗した場合 */
    case CommandFailed(cmd) => {
      log.error(cmd.toString + " failed")
      context stop self
    }
  }

}

case object Stop
