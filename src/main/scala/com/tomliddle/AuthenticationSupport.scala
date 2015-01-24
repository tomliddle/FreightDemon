package com.tomliddle

import org.scalatra.ScalatraBase
import org.scalatra.auth.{ScentryConfig, ScentrySupport}
import org.slf4j.LoggerFactory
import Tables._
import scala.slick.jdbc.JdbcBackend.Database

trait AuthenticationSupport extends ScalatraBase with ScentrySupport[User] {
	//self: ScalatraBase =>

	var db: DatabaseSupport = _

	protected def fromSession = {
		case id: String => {
			//db.getUser(_, id.toString).getOrElse(null)
			User("", "", "")
		}
	}

	protected def toSession = {
		case usr: User => usr.id.get.toString
	}

	protected val scentryConfig = (new ScentryConfig {
		override val login = "/sessions/new"
	}).asInstanceOf[ScentryConfiguration]

	protected val logger = LoggerFactory.getLogger(getClass)

	protected def requireLogin() = {
		if (!isAuthenticated) {
			redirect(scentryConfig.login)
		}
	}

	/**
	 * If an unauthenticated user attempts to access a route which is protected by Scentry,
	 * run the unauthenticated() method on the UserPasswordStrategy.
	 */
	override protected def configureScentry = {
		scentry.unauthenticated {
			scentry.strategies("UserPassword").unauthenticated()
		}
	}

	/**
	 * Register auth strategies with Scentry. Any controller with this trait mixed in will attempt to
	 * progressively use all registered strategies to log the user in, falling back if necessary.
	 */
	override protected def registerAuthStrategies = {
		scentry.register("UserPassword", app => new UserPasswordStrategy(app, db))
		scentry.register("RememberMe", app => new RememberMeStrategy(app, db))
	}
}
