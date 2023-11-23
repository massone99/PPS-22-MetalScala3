package model.ecs.systems

trait TempoSystem:
  def update(time: Long): Unit
