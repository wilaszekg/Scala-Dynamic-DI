import akka.actor.SupervisorStrategy.{Decider, Stop}
import akka.actor._
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.util.Timeout
import com.github.wilaszekg.scaladdi.akka.{DynamicConfigurationFailure, ActorDependency, ProxyProps}
import com.github.wilaszekg.scaladdi.Dependencies
import model._
import org.scalatest.{Matchers, WordSpecLike}

import scala.concurrent.duration._
import scala.language.{implicitConversions, postfixOps}

class ProxyTest extends TestKit(ActorSystem("test-system")) with WordSpecLike with ImplicitSender with Matchers {

  import scala.concurrent.ExecutionContext.Implicits.global

  private val timeoutDuration: FiniteDuration = 100 millis
  implicit val timeout = Timeout(timeoutDuration)

  def actor(prods: Products): Props = Props(new PriceCalculator(prods))

  "Proxied actor" should {

    val basketKeeper = new TestProbe(system)
    case class AskForProducts(basket: ImprovedBasket)

    val workingDependencies = Dependencies().withFuture(findShop("Bakery"))
      .withVal(User("John"))
      .requires(basketDependency)
      .requires(improvedBasketDependency)
      .requires(ActorDependency(basketKeeper.ref, (b: ImprovedBasket) => AskForProducts(b), classOf[Products]))

    "be started and answer" in {
      val proxyProps = new ProxyProps(actor _)
      val props = proxyProps from workingDependencies
      val proxy = system.actorOf(props)

      basketKeeperReply()
      checkBasketPrice(proxy)
    }

    "retry executing dependencies and succeed" in {
      val proxyProps = new ProxyProps(actor _)
      val userFinder = failingUserFinder(2)
      val d = failingDependencies(userFinder)
      val props = proxyProps from d

      val proxy = system.actorOf(props)

      basketKeeperReply()
      checkBasketPrice(proxy)
    }

    "send failure" in {
      class ParentExecutor extends Actor {
        var testReceiver: ActorRef = _

        override def receive: Receive = {
          case "start" => testReceiver = sender
            val proxyProps = new ProxyProps(actor _, dependenciesTriesMax = Some(3))
            val userFinder = failingUserFinder(3)
            val d = failingDependencies(userFinder)
            val props = proxyProps from d
            context.actorOf(props)

          case x => testReceiver ! x
        }
      }

      system.actorOf(Props(new ParentExecutor)) ! "start"
      expectMsgClass(classOf[DynamicConfigurationFailure])
    }

    "stop after configuration failure" in {
      val proxyProps = new ProxyProps(actor _, dependenciesTriesMax = Some(1))
      val userFinder = failingUserFinder(1)
      val d = failingDependencies(userFinder)
      val props = proxyProps from d

      val proxy = watch(system.actorOf(props))

      expectTerminated(proxy)
    }

    "terminate proxy" in {
      val proxyProps = new ProxyProps(actor _, supervisionStrategy = alwaysStopStrategy, reConfigureAfterTerminated = false)
      val props = proxyProps from workingDependencies
      val proxy = watch(system.actorOf(props))
      basketKeeperReply()

      proxy ! KillCalculator

      expectTerminated(proxy)
    }

    "re-configure proxied actor" in {
      val proxyProps = new ProxyProps(actor _, supervisionStrategy = alwaysStopStrategy, reConfigureAfterTerminated = true)
      val props = proxyProps from workingDependencies
      val proxy = watch(system.actorOf(props))
      basketKeeperReply()

      proxy ! KillCalculator

      basketKeeperReply()
      checkBasketPrice(proxy)
    }

    def failingUserFinder(failures: Int) =
      ActorDependency(system.actorOf(Props(new FailingUserFinder(failures))),
        (name: String) => FindUser(name),
        classOf[User])

    def failingDependencies(userFinder: ActorDependency[Tuple1[String], User]) = {
      Dependencies().withFuture(findShop("Bakery")).withVal("John")
        .requires(userFinder)
        .requires(basketDependency)
        .requires(improvedBasketDependency)
        .requires(ActorDependency(basketKeeper.ref, (b: ImprovedBasket) => AskForProducts(b), classOf[Products]))
    }

    def alwaysStopStrategy = {
      def defaultDecider: Decider = {
        case x => Stop
      }
      OneForOneStrategy()(defaultDecider)
    }

    def basketKeeperReply(): Unit = {
      basketKeeper.expectMsg(AskForProducts(ImprovedBasket(Basket(User("John"), Shop("Bakery")))))
      basketKeeper.reply(Products(List(Product(5), Product(10))))
    }

    def checkBasketPrice(proxy: ActorRef) = {
      proxy ! Calculate
      expectMsg(Price(15))
    }
  }
}
