/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2003-2011, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

package scala.concurrent

/** Promise is an object which can be completed with a value or failed
 *  with an exception.
 *
 *  @define promiseCompletion
 *  If the promise has already been fulfilled, failed or has timed out,
 *  calling this method will throw an IllegalStateException.
 *
 *  @define allowedThrowables
 *  If the throwable used to fail this promise is an error, a control exception
 *  or an interrupted exception, it will be wrapped as a cause within an
 *  `ExecutionException` which will fail the promise.
 *
 *  @define nonDeterministic
 *  Note: Using this method may result in non-deterministic concurrent programs.
 */
trait Promise[T] {

  /** Future containing the value of this promise.
   */
  def future: Future[T]

  /** Completes the promise with either an exception or a value.
   *
   *  @param result     Either the value or the exception to complete the promise with.
   *
   *  $promiseCompletion
   */
  def complete(result: Either[Throwable, T]): this.type = if (tryComplete(result)) this else throwCompleted

  /** Tries to complete the promise with either a value or the exception.
   *
   *  $nonDeterministic
   *
   *  @return    If the promise has already been completed returns `false`, or `true` otherwise.
   */
  def tryComplete(result: Either[Throwable, T]): Boolean

  /** Completes this promise with the specified future, once that future is completed.
   *
   *  @return   This promise
   */
  final def completeWith(other: Future[T]): this.type = {
    other onComplete {
      this complete _
    }
    this
  }

  /** Completes the promise with a value.
   *
   *  @param value    The value to complete the promise with.
   *
   *  $promiseCompletion
   */
  def success(v: T): this.type = if (trySuccess(v)) this else throwCompleted

  /** Tries to complete the promise with a value.
   *
   *  $nonDeterministic
   *
   *  @return    If the promise has already been completed returns `false`, or `true` otherwise.
   */
  def trySuccess(value: T): Boolean = tryComplete(Right(value))

  /** Completes the promise with an exception.
   *
   *  @param t        The throwable to complete the promise with.
   *
   *  $allowedThrowables
   *
   *  $promiseCompletion
   */
  def failure(t: Throwable): this.type = if (tryFailure(t)) this else throwCompleted

  /** Tries to complete the promise with an exception.
   *
   *  $nonDeterministic
   *
   *  @return    If the promise has already been completed returns `false`, or `true` otherwise.
   */
  def tryFailure(t: Throwable): Boolean = tryComplete(Left(t))

  /** Wraps a `Throwable` in an `ExecutionException` if necessary. TODO replace with `resolver` from scala.concurrent
   *
   *  $allowedThrowables
   */
  protected def wrap(t: Throwable): Throwable = t match {
    case t: Throwable if isFutureThrowable(t) => t
    case _ => new ExecutionException(t)
  }

  private def throwCompleted = throw new IllegalStateException("Promise already completed.")

}



object Promise {

  /** Creates a new promise.
   */
  def apply[T]()(implicit executor: ExecutionContext): Promise[T] = new impl.Promise.DefaultPromise[T]()

  /** Creates an already completed Promise with the specified exception
   */
  def failed[T](exception: Throwable)(implicit executor: ExecutionContext): Promise[T] = new impl.Promise.KeptPromise[T](Left(exception))

  /** Creates an already completed Promise with the specified result
   */
  def successful[T](result: T)(implicit executor: ExecutionContext): Promise[T] = new impl.Promise.KeptPromise[T](Right(result))
  
}









