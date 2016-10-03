package com.github.wilaszekg.scaladdi

import shapeless.ops.function.FnToProduct

case class FunctionDependency[F: FnToProduct](function: F)
