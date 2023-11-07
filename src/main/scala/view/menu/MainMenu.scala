package view.menu

import javafx.event.{ActionEvent, EventHandler}
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.input.{KeyCode, KeyEvent}
import javafx.scene.layout.{GridPane, Pane}
import javafx.scene.paint.{Color, PhongMaterial}
import javafx.scene.shape.Box
import javafx.stage.Stage
import model.ecs.components.{ColliderComponent, GravityComponent, PositionComponent, Size}
import model.ecs.entities.{EntityManager, PlayerEntity}
import model.ecs.systems.Systems.{gravitySystem, inputMovementSystem}
import model.ecs.systems.{CollisionSystem, SystemManager, Systems}
import model.engine.Engine
import view.{GameView, View}

trait MainMenu extends View:
  def getButton(root: Pane, buttonText: String): Button =
    root.getChildren
      .filtered {
        case btn: Button if btn.getText == buttonText => true
        case _                                        => false
      }
      .get(0)
      .asInstanceOf[Button]

  def startButton: Button

  def exitButton: Button

  def handleStartButton(): Unit

  def handleExitButton(): Unit

private class MainMenuImpl(parentStage: Stage) extends MainMenu:

  val loader: FXMLLoader = FXMLLoader(getClass.getResource("/main.fxml"))
  val root: GridPane = loader.load[javafx.scene.layout.GridPane]()

  private val entityManager = EntityManager()
  private val systemManager = SystemManager(entityManager)
  private val gameEngine = Engine(systemManager)
  getButton(root, "Start").setOnAction((_: ActionEvent) => handleStartButton())
  getButton(root, "Exit").setOnAction((_: ActionEvent) => handleExitButton())

  def handleStartButton(): Unit =
    val gameView =
      GameView(parentStage, Set(entityManager, Systems, gameEngine))
    entityManager
      .addEntity(
        PlayerEntity()
          .addComponent(PositionComponent(100, 100))
          .addComponent(GravityComponent(model.GRAVITY_VELOCITY))
          .addComponent(ColliderComponent(Size(50, 50)))
      )
      .addEntity(
        PlayerEntity()
          .addComponent(PositionComponent(200, 100))
          .addComponent(GravityComponent(model.GRAVITY_VELOCITY))
          .addComponent(ColliderComponent(Size(50, 50)))
      )
    systemManager
      .addSystem(inputMovementSystem)
      .addSystem(gravitySystem)
      .addSystem(entityManager => CollisionSystem(entityManager))
    parentStage.getScene.setRoot(gameView)
    gameEngine.start()

  def handleExitButton(): Unit =
    parentStage.close()
    gameEngine.stop()

  override def startButton: Button = getButton(root, "Start")

  override def exitButton: Button = getButton(root, "Exit")

object MainMenu:
  def apply(parentStage: Stage): MainMenu =
    MainMenuImpl(parentStage)
