package io.github.morgaroth.msc.quide.core.model

import org.scalatest.{Matchers, WordSpec}

/**
  * Created by mateusz on 06.01.16.
  */
class OperatorTest extends WordSpec with Matchers {

  import QbitValue._
  import Complex._

  "A Identity operator" should {
    "leave |0> qbit without changes" in {
      `|0>`(I) should equal(`|0>`)
      I(`|0>`) should equal(`|0>`)
    }
    "leave |1> qbit without changes" in {
      `|1>`(I) should equal(`|1>`)
      I(`|1>`) should equal(`|1>`)
    }
  }

  "A Hadammard gate" should {
    "be correctly applied to |0>" in {
      `|0>`(H) should equal(QbitValues.`1/p2 1/p2`)
      H(`|0>`) should equal(QbitValues.`1/p2 1/p2`)
    }
    "be correctly applied to |1>" in {
      `|1>`(H) should equal(QbitValues.`1/p2 -1/p2`)
      H(`|1>`) should equal(QbitValues.`1/p2 -1/p2`)
    }
  }

  "A Pauli X gate" should {
    "be correctly applied to |0>" in {
      `|0>`(X) should equal(`|1>`)
      X(`|0>`) should equal(`|1>`)
    }
    "be correctly applied to |1>" in {
      `|1>`(X) should equal(`|0>`)
      X(`|1>`) should equal(`|0>`)
    }
  }
}
