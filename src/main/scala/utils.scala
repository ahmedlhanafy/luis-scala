import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

package object utils {
  def getSuccessfulFutures[T](
    futures: Seq[Future[T]]
  )(implicit executionContext: ExecutionContext): Future[Seq[T]] =
    Future
      .sequence(futures.map(_.transform(Try(_))))
      .map(_.collect { case Success(x) => x })
}
