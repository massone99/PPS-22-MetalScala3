import javafx.scene.input.KeyCode
import javafx.scene.paint.Color
import model.ecs.components.{Component, GravityComponent, PositionComponent}
import model.ecs.entities.{Entity, EntityManager, PlayerEntity}
import model.ecs.systems.SystemManager
import model.ecs.systems.Systems.{gravitySystem, inputMovementSystem, passiveMovementSystem}
import model.event.observer.Observable
import model.utilities.{Cons, Empty, Stack}

import scala.collection.mutable
import scala.reflect.{ClassTag, classTag}

package object model:
  val GUIWIDTH: Int = 1500
  val GUIHEIGHT: Int = 800

  val INPUT_MOVEMENT_VELOCITY = 35
  val JUMP_MOVEMENT_VELOCITY = 250
  val GRAVITY_VELOCITY = 10

  val VERTICAL_COLLISION_SIZE = 100
  val HORIZONTAL_COLLISION_SIZE = 100

  var inputsQueue: Stack[KeyCode] = Empty

