import akka.io.{PipelineStage,PipelineContext,PipePair}
import akka.io.{StringByteStringAdapter,DelimiterFraming,TcpReadWriteAdapter}
import akka.io.Tcp
import akka.util.ByteString

/**
 * 合成したパイプラインを返す
 */
object ServerPipelineStage {
  def apply(): PipelineStage[PipelineContext,Command,Tcp.Command,Event,Tcp.Event] = {
    new ServerPipelineStage >>
    new StringByteStringAdapter("utf-8") >>
    new DelimiterFraming(maxSize = 1024, delimiter = ByteString("\r\n")) >>
    new TcpReadWriteAdapter
  }
}

/**
 * ユーザ定義のコマンド・イベントと文字列への変換を定義
 */
class ServerPipelineStage extends PipelineStage[PipelineContext, Command, String, Event, String] {

  /* applyメソッドは変換の実装を含むPipePairを返す */
  def apply(ctx: PipelineContext) = new PipePair[Command, String, Event, String] {
    /* バッファなどはここに作れる */
    // var buffer: String = ""

    /* コマンドを文字列に変換(返す要素がない場合は ctx.nothing を返り値にする) */
    override def commandPipeline = { cmd: Command =>
      // MEMO: DelimiterFramingがコマンドに対してデリミタを追加してくれない (2.2.0-RC1)
      def singleCommand(str: String) = ctx.singleCommand(str + "\r\n")

      cmd match {
        case Pong             => singleCommand("pong")
        case Quitted          => singleCommand("Bye!")
        case SomeCommand(mes) => singleCommand("Unknown command: " + mes)
      }
    }

    /* 文字列をイベントに変換 */
    override def eventPipeline = (_: String).toLowerCase match {
      case "ping" => ctx.singleEvent(Ping)
      case "quit" => ctx.singleEvent(Quit)
      case mes    => ctx.singleEvent(SomeEvent(mes))
    }
  }
}
