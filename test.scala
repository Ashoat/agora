import CoroutineLoader._
import DataObject._
import scala.util.continuations._

object Test {

  def getFive() = reset {
    val seq: Seq[Any] = shift { fetch[Int](DataObject.get(15181990251L)) }
    val Seq(obj: DataObject) = seq
    Result(obj.fields.get("test").toInt)
  }

  def getEight()  = reset {
    val seq: Seq[Any] = shift { fetch[Int](getFive(), Result(3)) }
    val Seq(a: Int, b: Int) = seq
    Result(a + b)
  }

}
