package model.ecs.systems

import javafx.scene.input.KeyCode
import model.*
import model.ecs.components.*
import model.ecs.entities.*
import model.ecs.entities.environment.BoxEntity
import model.ecs.entities.player.PlayerEntity
import model.ecs.entities.weapons.{BulletEntity, MachineGunEntity, WeaponEntity}
import model.ecs.systems.CollisionChecker.{boundaryCheck, getCollidingEntity, isOutOfHorizontalBoundaries}
import model.ecs.systems.Systems.getUpdatedPosition
import model.event.Event
import model.event.observer.Observable
import model.input.commands.*
import model.utilities.Empty

import scala.reflect.ClassTag

object Systems extends Observable[Event]:

  val bulletMovementSystem: Long => Unit = elapsedTime =>
    EntityManager().getEntitiesByClass(classOf[BulletEntity]).foreach {
      bullet =>
        given position: PositionComponent =
          bullet.getComponent[PositionComponent].get
        given velocity: VelocityComponent =
          bullet.getComponent[VelocityComponent].get

        val proposedPosition = getUpdatedPosition(elapsedTime)
        bullet.handleCollision(proposedPosition) match
          case Some(handledPosition) => bullet.replaceComponent(handledPosition)
          case None                  => ()
    }

  val inputMovementSystem: Long => Unit = * =>
    EntityManager().getEntitiesWithComponent(classOf[PlayerComponent]).foreach {
      entity =>
        inputsQueue.peek match
          case Some(command) => command(entity)
          case None          => ()
        inputsQueue = inputsQueue.pop.getOrElse(Empty)
    }

  val gravitySystem: Long => Unit = elapsedTime =>
    if (model.isGravityEnabled) {
      EntityManager()
        .getEntitiesWithComponent(
          classOf[PositionComponent],
          classOf[GravityComponent],
          classOf[VelocityComponent]
        )
        .foreach { entity =>
          val position = entity.getComponent[PositionComponent].get
          val velocity = entity.getComponent[VelocityComponent].get
          val isTouchingGround =
            position.y + VERTICAL_COLLISION_SIZE >= model.GUIHEIGHT && velocity.y >= 0
          if isTouchingGround then
            entity.replaceComponent(VelocityComponent(velocity.x, 0))
          else
            entity.replaceComponent(
              velocity + VelocityComponent(0, GRAVITY_VELOCITY * elapsedTime)
            )
        }
    }

  def getUpdatedPosition(elapsedTime: Long)(using
      position: PositionComponent,
      velocity: VelocityComponent
  ): PositionComponent = {
    val newPositionX = position.x + velocity.x * elapsedTime * 0.001
    val newPositionY = position.y + velocity.y * elapsedTime * 0.001

    PositionComponent(newPositionX, newPositionY)
  }

  private def getUpdatedVelocity(entity: Entity)(using
      velocity: VelocityComponent
  ): VelocityComponent = {
    val newHorizontalVelocity = velocity.x * FRICTION_FACTOR match {
      case x if -0.1 < x && x < 0.1 => 0.0
      case x                        => x
    }
    entity match {
      case _: PlayerEntity =>
        val sprite = velocity match {
          case VelocityComponent(0, 0)           => model.marcoRossiSprite
          case VelocityComponent(_, y) if y != 0 => model.marcoRossiJumpSprite
          case VelocityComponent(x, y) if x != 0 && y == 0 =>
            model.marcoRossiMoveSprite
        }
        entity.replaceComponent(SpriteComponent(sprite))
      case _: BulletEntity =>
        entity.replaceComponent(SpriteComponent("sprites/Bullet.png"))
      case _: MachineGunEntity =>
        entity.replaceComponent(SpriteComponent("sprites/h.png"))
      case _: BoxEntity =>
        entity.replaceComponent(SpriteComponent("sprites/box.jpg"))
    }

    VelocityComponent(newHorizontalVelocity, velocity.y)
  }

  private def updateJumpingState(entity: Entity): Unit = {
    if entity.hasComponent(classOf[PlayerComponent])
    then
      val currentPosition = entity
        .getComponent[PositionComponent]
        .getOrElse(throw new Exception("Position not found"))
      val velocity = entity
        .getComponent[VelocityComponent]
        .getOrElse(throw new Exception("Velocity not found"))
      val isTouchingGround =
        currentPosition.y + VERTICAL_COLLISION_SIZE >= model.GUIHEIGHT && velocity.y >= 0
      if (isTouchingGround)
        // fixme: gravity should be subjective
        model.isGravityEnabled = false
        entity.replaceComponent(JumpingComponent(false))
      else
        model.isGravityEnabled = true
        entity.getComponent[JumpingComponent].get
  }

  val positionUpdateSystem: Long => Unit = elapsedTime =>
    EntityManager()
      .getEntitiesWithComponent(
        classOf[PositionComponent],
        classOf[VelocityComponent],
        classOf[JumpingComponent]
      )
      .foreach { entity =>
        given currentPosition: PositionComponent =
          entity.getComponent[PositionComponent].get
        given currentVelocity: VelocityComponent =
          entity.getComponent[VelocityComponent].get

        entity.replaceComponent(getUpdatedVelocity(entity))
        updateJumpingState(entity)

        val proposedPosition = getUpdatedPosition(elapsedTime)
        val handledPosition = entity.handleCollision(proposedPosition)
        handledPosition match
          case Some(handledPosition) => entity.replaceComponent(handledPosition)
          // keep the current position
          case None => ()
      }
