package io.javalin.misc

import io.javalin.Javalin
import org.junit.Test

class StaticDirectorySlashRuntimeTest {
	
	@Test
	fun `runtime test`() {
		val normal = Javalin.create()
				.enableStaticFiles("public")
				.start(1000)
		
		val nonIgnoring = Javalin.create()
				.enableStaticFiles("public")
				.dontIgnoreTrailingStaticDirectorySlashes()
				.start(1001)
		
		while (true) { Thread.sleep(1000) } // sleep test
	}
	
}