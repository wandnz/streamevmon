package nz.net.wand.streamevmon.tuner

import nz.net.wand.streamevmon.TestBase
import nz.net.wand.streamevmon.tuner.jobs.{Job, JobResult, SimpleJob}
import nz.net.wand.streamevmon.tuner.ActorManager.{JobResultHook, RemoveJobResultHook}

import scala.concurrent.{Await, Future, TimeoutException}
import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.ExecutionContext.Implicits.global

class AkkaTest extends TestBase {
  "Akka wrappers" should {
    "execute code when submitted to" in {
      var jobHasExecuted = false

      ConfiguredPipelineRunner.submit(new Job("test-job") {
        private val self: Job = this

        override def run(): JobResult = {
          jobHasExecuted = true
          new JobResult {
            override val job: Job = self
          }
        }
      })

      Thread.sleep(1000)
      assert(jobHasExecuted)
    }

    "execute job result hooks" in {
      var hookHasExecuted = false

      ConfiguredPipelineRunner.addJobResultHook(
        _ => hookHasExecuted = true
      )

      ConfiguredPipelineRunner.submit(SimpleJob("hello-world"))

      // This will wait until hookHasExecuted goes true, or for 1 second, whichever comes first.
      try {
        Await.result(
          Future(while (!hookHasExecuted) {}),
          Duration(1, SECONDS)
        )
      }
      catch {
        case _: TimeoutException =>
        case e: Throwable => throw e
      }

      assert(hookHasExecuted)
    }

    "remove job result hooks" in {
      var hookHasExecuted = false

      val hook = new JobResultHook {
        override def apply(jr: JobResult): Unit = hookHasExecuted = true
      }

      ConfiguredPipelineRunner.addJobResultHook(hook)
      ConfiguredPipelineRunner.removeJobResultHook(RemoveJobResultHook(hook))

      ConfiguredPipelineRunner.submit(SimpleJob("hello-world"))

      try {
        Await.result(
          Future(while (!hookHasExecuted) {}),
          Duration(1, SECONDS)
        )
      }
      catch {
        case _: TimeoutException =>
        case e: Throwable => throw e
      }
      assert(!hookHasExecuted)
    }
  }
}
