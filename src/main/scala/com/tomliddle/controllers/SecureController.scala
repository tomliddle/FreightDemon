package com.tomliddle.controllers


import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem}
import akka.util.Timeout
import com.tomliddle.auth.AuthenticationSupport
import com.tomliddle.database.{MongoSupport, DatabaseSupport}
import com.tomliddle.entity.LocationMatrix
import com.tomliddle.form.{SolutionForm, StopForm, TruckForm}
import com.tomliddle.solution.timeanddistance.Geocoding
import com.tomliddle.solution.Solution
import org.bson.types.ObjectId
import org.joda.time.LocalTime
import org.joda.time.format.DateTimeFormat
import org.json4s.JsonAST.JString
import org.json4s._
import org.scalatra.json.JacksonJsonSupport
import org.scalatra.servlet.{FileUploadSupport, MultipartConfig, SizeConstraintExceededException}
import org.scalatra.{FutureSupport, RequestEntityTooLarge}

import scala.concurrent.ExecutionContext

class SecureController(protected val db: DatabaseSupport, mdb: MongoSupport, system: ActorSystem, myActor: ActorRef)
		extends ScalateServlet with Geocoding with FutureSupport with FileUploadSupport with AuthenticationSupport with JacksonJsonSupport {

	configureMultipartHandling(MultipartConfig(maxFileSize = Some(3 * 1024 * 1024)))

	private final val formatter = DateTimeFormat.forPattern("HH:mm")

	case object LocalTimeSerialiser extends CustomSerializer[LocalTime](format => ({
			case JString(s) => LocalTime.parse(s, formatter)
			case JNull => null
		},{
			case x: LocalTime => JString(x.toString(formatter))
		})
	)

	protected implicit val timeout = Timeout(5, TimeUnit.SECONDS)
	protected implicit val jsonFormats: Formats = DefaultFormats + LocalTimeSerialiser
	protected implicit def executor: ExecutionContext = system.dispatcher


	before() {
		requireLogin()
	}

	//************** Geocode HANDLING ******************************
	get("/geocode/:address") {
		contentType = formats("json")
		geocodeFromOnline(params("address"))
	}

	//************** Truck HANDLING ******************************
	// Add truck
	post("/truck") {
		var truckForm = parsedBody.extract[TruckForm]
		db.addTruck(truckForm.getTruck(scentry.user.id.get))
	}

	// Get truck
	get("/truck/:id") {
		contentType = formats("json")
		db.getTruck(params("id").toInt, scentry.user.id.get)
	}

	// Get truck for that user
	get("/truck") {
		contentType = formats("json")
		db.getTrucks(scentry.user.id.get)
	}

	// Delete truck
	delete("/truck/:id") {
		db.deleteTruck(params("id").toInt, scentry.user.id.get)
	}

	//************** Stop HANDLING ******************************
	// Add stop
	post("/stop") {
		val stop = parsedBody.extract[StopForm]

		db.addStop(stop.getStop(scentry.user.id.get))
	}

	// Get stop
	get("/stop/:id") {
		contentType = formats("json")
		db.getStop(params("id").toInt, scentry.user.id.get)
	}

	// Get stop for that user
	get("/stop") {
		contentType = formats("json")
		db.getStops(scentry.user.id.get)
	}

	// Delete stop
	delete("/stop/:id") {
		db.deleteStop(params("id").toInt, scentry.user.id.get)
	}


	// ****************************** SOLUTION *********************

	post("/solution") {
		var solution = parsedBody.extract[SolutionForm]
		val lm = new LocationMatrix(List(), List())
		mdb.addSolution(Solution(solution.name, db.getDepots(scentry.user.id.get).head, List(), List(), lm, scentry.user.id.get))
	}

	get("/solution") {
		contentType = formats("json")

		val userId = scentry.user.id.get
		val depots = db.getDepots(userId)
		val stops = db.getStops(userId)
		val lm = new LocationMatrix(stops, depots)

		val solutions = mdb.getSolutions(scentry.user.id.get).map {
			sol => sol.copy(stopsToLoad = stops, depot = depots.head, trucks = db.getTrucks(userId).map(_.toTruck(List(), depots.head, lm)), lm = lm).preload.shuffle
		}
		solutions
	}

	get("/solution/:name") {
		contentType = formats("json")
		val userId = scentry.user.id.get
		mdb.getSolution(userId, params("name"))

		val depots = db.getDepots(userId)
		val stops = db.getStops(userId)
		val lm = new LocationMatrix(stops, depots)
		Solution(params("name"), depots.head, stops, db.getTrucks(userId).map(_.toTruck(List(), depots.head, lm)), lm, userId)
	}

	delete("/solution/:name") {
		contentType = formats("json")
		mdb.removeSolution(scentry.user.id.get, params("name"))
	}

	////////////////////


	get("/solution/run/:id") {
		contentType = formats("json")
	}



	//****************************** OTHER *************************


	get("/") {
		contentType = "text/html"
		ssp("/home")
	}

	error {
		case e: SizeConstraintExceededException => RequestEntityTooLarge("file is too big")
	}

}
