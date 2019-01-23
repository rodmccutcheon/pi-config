package com.rodmccutcheon.pi.config

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class SshParamsTest extends Specification {
  "SshParams" should {
    "Parse a correct string" in {
      SshParams.parse("user123#$%^_-?:pwd456#$%^_-?@10.9.8.7") must_=== SshParams("user123#$%^_-?", "pwd456#$%^_-?", "10.9.8.7")
    }
  }
}
