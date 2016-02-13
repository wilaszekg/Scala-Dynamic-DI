package model

case class User(name: String)

case class Shop(name: String)

case class Basket(user: User, shop: Shop)

case class ImprovedBasket(basket: Basket)

case class Product(price: Int)

case class Products(products: List[Product])

case class Price(value: Int)
