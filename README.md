# Scala-Dynamic-DI
This is a library for type-safe, boilerplate-free dynamic dependency injection for Akka actors.

## The problem of dynamic dependency injection in Akka actors
We often want to dynamically get some data when an actor is starting. It often looks like this:
```scala
class ConfPriceCalculator(user: User, shopId: String,
  findShop: String => Future[Shop],
  findBasket: (User, Shop) => Future[Basket],
  improveBasket: Basket => ImprovedBasket,
  basketKeeper: ActorRef) extends Actor {
  
  override def preStart(): Unit = {
    super.preStart()
    for {
      shop <- findShop(shopId)
      basket <- findBasket(user, shop)
    } self ! improveBasket(basket)
  }

  override def receive: Receive = {
    case improvedBasket: ImprovedBasket =>
      basketKeeper ! AskForProducts(improvedBasket)
      context become askedForProducts
  }

  def askedForProducts: Receive = {
    case products: Products => context become work(products)
  }

  def work(products: Products): Receive = {
    case Calculate => sender ! Price(products.products.foldLeft(0)(_ + _.price))
  }
}
```
You have to compose some futures, call some functions, ask other actors to finally start the work of your actor. It turns out that the majority of the actor is boilerplate code and it obscures the real behaviour. What we really want in this case is:
```scala
class ConfPriceCalculator(products: Products) extends Actor {
  override def receive: Receive = {
    case Calculate => sender ! Price(products.products.foldLeft(0)(_ + _.price))
  }
}
```
Looks much simpler. But how to achieve it and where to prepare all these dependencies for the actor?

## How to solve this problem
You can design a more elegant solution but with this library the actor construction is very easy and straightforward. You can create the props of the actor this way:
```scala
val props = Dependencies().withFuture(findShop(shopId))
  .withVal(user)
  .requires(FutureDependency((user: User, shop: Shop) => Future(Basket(user, shop))))
  .requires(FunctionDependency((basket: Basket) => ImprovedBasket(basket)))
  .requires(ActorDependency(basketKeeper.ref, products _, classOf[Products])).props(PriceCalculator.props)
```

You can declare the dependencies (`FutureDependency`, `FunctionDependency` or `ActorDependency`) in one place and reuse them. Everything is completely type-safe. If you append a new dependency to be built, the library ensures that all required parameters are available in the scope. Under the hood it composes all dependencies as `Futures` and pass a final `Future` to a proxy actor. The proxy actor creates the destination actor once all dependencies are available. This way your actor can be very simple and requires only the parameters that it really uses.

