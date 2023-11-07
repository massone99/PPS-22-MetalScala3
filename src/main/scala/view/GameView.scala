package view

import javafx.application.Platform
import javafx.scene.layout.{Border, BorderStroke, BorderStrokeStyle, FlowPane, Pane}
import javafx.scene.paint.{Color, PhongMaterial}
import javafx.scene.shape.Box
import javafx.scene.{Node, Scene}
import javafx.stage.Stage
import model.ecs.components.*
import model.ecs.entities.{Entity, EntityManager}
import model.event.Event
import model.event.Event.{Move, Spawn, Tick}
import model.event.observer.{Observable, Observer}
import model.input.BasicInputHandler

import java.util.UUID

trait GameView extends View

private class GameViewImpl(primaryStage: Stage, observables: Set[Observable[Event]]) extends GameView with BasicInputHandler with Observer[Event] {
  val root: Pane = Pane()
  private var entityIdToView: Map[UUID, Node] = Map()

  //Creazione della scena di gioco (Diversa da quella del Menù).
  private val scene: Scene = Scene(root, model.GUIWIDTH, model.GUIHEIGHT)
  scene.setOnKeyPressed(handleInput)
  primaryStage.setScene(scene)
  observables.foreach(_.addObserver(this))

  override def update(subject: Event): Unit =
    Platform.runLater { () =>
      subject match
        case Spawn(entity, ofType, position) =>
          entityIdToView = entityIdToView + (entity -> createBoxView(position))
        case Move(entity, position) =>
          val box = entityIdToView(entity)
          box.setTranslateX(position.x)
          box.setTranslateY(position.y)
         // print(entity.toString + " " + box.getLayoutX + "\n")
        case Tick() =>
          entityIdToView.foreach((_, view) => root.getChildren.remove(view))
          entityIdToView.foreach((_, view) => root.getChildren.add(view))
    }

  private def createBoxView(position: PositionComponent): Node =
    val box = Box(100, 100, 100)
    box.setTranslateX(position.x)
    box.setTranslateY(position.y)
    box.setMaterial(PhongMaterial(Color.BLACK))
    box

}

object GameView {
  def apply(primaryStage: Stage, observables: Set[Observable[Event]]): GameView =
    new GameViewImpl(primaryStage, observables)
}
