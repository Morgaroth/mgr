package io.github.morgaroth.msc.quide.core.register

import io.github.morgaroth.msc.quide.core.actors.QuideActor

/**
  * Created by morgaroth on 14.04.2016.
  */
class History extends QuideActor {
  val tasks = collection.mutable.Map.empty[Long, QState.Execute]
  var currentNo = 0l


  override def receive: Receive = {
    case task@QState.Execute(_, no) =>
      log.info(s"storing task no $no")
      tasks += no -> task
      currentNo = math.max(currentNo, no)
    case no: Long =>
      log.info(s"returning task $no to ${sender().path}")
      tasks.get(no).map(sender() ! _).getOrElse {
        log.warning(s"${sender()} asked for non existing task!")
      }
    case (from: Long, to: Long) =>
      tasks.filterKeys(x => x > from && x < to).values.foreach(sender() ! _)
  }
}
