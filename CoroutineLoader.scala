import scala.collection.mutable
import scala.util.continuations._

/**
 * This module defines a paradigm for data fetching in web applications. The
 * idea is to represent all functions that rely on blocking IO for data fetches
 * as coroutines that yield when they can't continue without a fetch. We then
 * collate their exection and execute a big batched data fetch in between each
 * step. To achieve the "yield" functionality we make use of Scala's CPS module.
 */
object CoroutineLoader {

  /**
   * Convenient shorthand for annotating the result type of coroutines.
   */
  type Coroutine[T] = cps[ExecState[T]]

  /**
   * Captured state of a function that yields an R.
   * First case is an incomplete state; second case is the final result.
   */
  abstract class ExecState[+R] {
    def isContinuation = this match {
      case Continuation(_) => true
      case Result(_) => false
    }
  }
  case class Continuation[+R](val next: Unit => ExecState[R]) 
     extends ExecState[R]
  case class Result[+R](val result: R)
     extends ExecState[R]

  /**
   * This function takes as input a list of ExecState dependencies and a function
   * that transforms the satisfied dependencies into another ExecState. It does
   * not compute any of the dependencies; it simply builds and returns a new
   * ExecState that prepends the computation of the dependencies to the
   * computation of the final result. This function will recursively call itself
   * for each dependency, so in effect we are taking a DAG of dependencies and
   * converting it into a linked list of computations that may occur in parallel.
   *
   * The point of executing the dependencies in parallel is that they all require
   * data fetches. By executing each dependency one step at a time, we are able to
   * coalesce all of these data fetches together into a single batch.
   *
   * @param first The list of dependencies that must be calculated.
   * @param after The callback that requires the satisfied dependencies.
   * @return      A continuation callback that is ready to execute.
   */
  def fetch[R](
    first: ExecState[Any]*
  ) : ((Seq[Any] => ExecState[R]) => ExecState[R]) = {
    // After the dependencies are specified we return a higher level function.
    // The function takes a Scala CPS function and is what we pass to shift.
    (after: Seq[Any] => ExecState[R]) => {
      // Don't actually execute everything. Instead, return a function that will
      // execute the next step and, in turn, return a function that will execute
      // the next step: essentially, a classic continuation.
      val nextStep = (_: Unit) => {
        if (first.filter(_.isContinuation).isEmpty) {
          // All done? Call the callback that was waiting on the dependencies.
          // We will return the ExecState that is returned by that callback.
          after(first.collect { case Result(x) => x })
        } else {
          // Execute a single step through each dependency.
          val states = first.map((state: ExecState[Any]) => {
            state match {
              case Continuation(next) =>
                next()
              case Result(_) =>
                state
            }
          })
          // We will call ourselves recursively to generate the next step.
          fetch(states: _*)(after)
        }
      } : ExecState[R]
      // Wrap the function that will execute the next step.
      Continuation[R](nextStep)
    }
  }

  /**
   * This function will take a list of ExecStates and will process them until
   * they're done. Between each step it will execute a data fetch.
   */
  def process(states: ExecState[Any]*) : Seq[Any] = {
    // If there are no Continuations left we are done!
    if (states.filter(_.isContinuation).isEmpty) {
      states.collect { case Result(x) => x }
    } else {
      // The whole point of all of this continuation junk. This call fetches
      // all the data necessary for this batched computation step in one go.
      DataObject.query()
      val processed = states.map((state: ExecState[Any]) => {
        state match {
          case Continuation(next) =>
            // If this isn't the final step in calculating this dependency, this
            // call will queue up data fetches that will be needed for the next
            // calculation step. The actual data fetch is executed above.
            next()
          case Result(_) =>
            state
        }
      })
      // Call ourselves recursively!
      process(processed: _*)
    }
  }

}
