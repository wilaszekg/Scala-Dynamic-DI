package com.github.wilaszekg.scaladdi

import shapeless.ops.function.FnToProduct

case class FutureDependency[F: FnToProduct](function: F)
