# Scala-Dynamic-DI
This is a library for type-safe, boilerplate-free dynamic dependency injection for Akka actors. It offers a DI model for actors dependent on asynchronous dependencie, like:
* `Future` calls
* messages received from other actors

# Using the dynamic DI

First, you have to create `ProxyProps` for the actor you want to inject dependencies into. You need to pass a function taking `ANYTHING` and returning Akka `Props`. The `ANYHTHING` defines dependencies of your actor. In this case it means that the actor `PriceCalculator` requires two dependencies of types `Products` and `Promotions`:
```scala
import com.github.wilaszekg.scaladdi.akkaProxyProps

new ProxyProps((products: Products, promotions: Promotions) => Props(new PriceCalculator(products, promotions)))
```

## Constructing dependencies
To start building dependencies for the actor, create a `Dependencies` object:
```scala
import com.github.wilaszekg.scaladdi.Dependencies

val dependencies = Dependencies()
```
It just creates empty dependencies.

### Using known defined values in dependencies
To construct dependencies further you can add a defined and already known value:
```scala
val user: User = ???

val dependencies = Dependencies().withVal(user)
```
or a a simple `Future` value:
```scala
val futureShop: Future[Shop] = ???

val dependencies = Dependencies().withVal(user).withFuture(futureShop)
```
The `Dependencies` constructed here holds dependencies with two types: `User` and `Shop`

### Using dependencies requiring other values
You can use three types od dependencies:
* `FutureDependency` - requires values of a some types and produces a `Future` of another type 
* `ActorDependency` - requires values of a some types and awaits for a response of another type from a given `ActorRef` 
* `FunctionDependency` - requires values of a some types and produces a value of another type 

#### Future Dependency
You create `FutureDependency` passing a function requiring some values and returning a `Future` of a value. This dependency requires `User` and `Shop` to be built and produces a `Basket`:

```scala
import com.github.wilaszekg.scaladdi.FutureDependency

def findBasket(user: User, shop: Shop): Future[Basket] = ???
val basketDependency = FutureDependency(findBasket _)
```
Now, you can add this dependency to an existing `Dependencies` object if it already contains required types: `User` and `Shop`:
```scala
val dependencies = Dependencies().withVal(user).withFuture(futureShop)
  .requires(basketDependency)
```

#### Function Dependency
`FunctionDependency` is very similar to `FutureDependency` but it uses a function producing a synchronous result. It may be a blocking call and it will be wrapped to a `Future`:
```scala
import com.github.wilaszekg.scaladdi.FunctionDependency

def findPromotions(shop: Shop): Promotions = ???
val promotionsDependency = FunctionDependency(findPromotions _)
val dependencies = Dependencies().withVal(user).withFuture(futureShop)
  .requires(basketDependency)
  .requires(promotionsDependency)
```

#### Actor Dependency
`ActorDependency` requires an `ActorRef` a class of expected response message (the type of the dependency) from the actor and a function creating answer message - arguments of this function define typrs required for this dependency:
```scala
import com.github.wilaszekg.scaladdi.akka.ActorDependency

val productsDependency = ActorDependency(basketActorRef, (b: Basket) => AskForProducts(b), classOf[Products])

val dependencies = Dependencies().withVal(user).withFuture(futureShop)
      .requires(basketDependency)
      .requires(promotionsDependency)
      .requires(productsDependency)
```

#### About the dependencies
We have built dependencies from known values `user`, `futureShop` and dyncamic dependencies `basketDependency`, `promotionsDependency` and `productsDependency`. Each dynamic dependency has some requirements and a produced type:
```scala
val basketDependency: FutureDependency[(User, Shop), Basket] = ...
val promotionsDependency: FunctionDependency[Tuple1[Shop], Promotions] = ...
val productsDependency: ActorDependency[Tuple1[Basket], Products] = ...
```
The required types are reflected as tuples. The produced value is always the second type argument.

It is crucial that `Dependencies` keeps lazy computation model of the futures so it will not run any future until you call its `run` method. This method is used by the proxy actor to start fetching the dependencies.

#### Type safety of dependencies
Whenever you add a new dependency using `requires` method, the compile will check the following conditions:
* all required types are present
* the produced type of the new dependency is not present in dependencies yet - it allows the compiler to control the types of all dependencies

### Starting the actor
If you have a `ProxyProps` and `Dependencies` instances, you have to use them to obtain a `Props` instance and start a proxy actor (using Akka context or system):
```scala
val props: Props = proxyProps from dependencies
val proxyActor: ActorRef = context.actorOf(props)
```

The proxy actor hides complexity of running future dependencies and allows you to implement simple actors code.

### Configuring the proxy
When you create `ProxyProps` instance you only need to pass a `propsFunction`. But you can configure it with some additional properties:
* `dependenciesTriesMax` - how many times the proxy will try to run the future dependencies if any of the future fails
* `reConfigureAfterTerminated` - specifies what happens if the target actor is terminated. Is set to `true`, the proxy will run the future dependencies once again to recover the actor
* `supervisionStrategy` - Akka supervision strategy for the target actor - specifies how the proxy will deal with its child proxied actor
* `dependencyError` - a function transforming an error to a message to be sent from the proxy to its parrent if fetching the future dependencies fails
* 
`ProxyProps` provides defaults for all these properties:
```scala
ProxyProps(propsFunction, dependenciesTriesMax: Option[Int] = None,
  reConfigureAfterTerminated: Boolean = true,
  supervisionStrategy: SupervisorStrategy = SupervisorStrategy.defaultStrategy,
  dependencyError: Throwable => Any = ProxyProps.defaultError)
```
