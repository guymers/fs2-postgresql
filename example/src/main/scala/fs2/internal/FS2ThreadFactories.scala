package fs2.internal

import java.util.concurrent.ThreadFactory

// internal only method, expose to example
object FS2ThreadFactories {

  def named(
    threadPrefix: String,
    daemon: Boolean
  ): ThreadFactory = ThreadFactories.named(threadPrefix, daemon)
}
