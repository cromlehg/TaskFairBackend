package tasks

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

import akka.actor.ActorSystem
import controllers.AppConstants
import javax.inject.Inject
import models.daos.DAO

class BaseActorTask @Inject() (actorSystem: ActorSystem, val dao: DAO)(implicit executionContext: ExecutionContext) {

  actorSystem.scheduler.schedule(initialDelay = 1.minutes, interval = AppConstants.INTERVAL_TASK.toInt.milliseconds) {

  }

  actorSystem.scheduler.schedule(initialDelay = 2.minutes, interval = AppConstants.INTERVAL_TASK.toInt.milliseconds) {

  }

}