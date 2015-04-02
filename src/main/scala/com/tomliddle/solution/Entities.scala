package com.tomliddle.solution

import org.joda.time.{Duration, LocalTime}


class DistanceTime(val distance: BigDecimal = BigDecimal(0), val time: Duration = new Duration(0)) {

	def +(operand: DistanceTime): DistanceTime = {
		new DistanceTime(this.distance + operand.distance, this.time.plus(operand.time))
	}

	def canEqual(other: Any): Boolean = other.isInstanceOf[DistanceTime]

	override def equals(other: Any): Boolean = other match {
		case that: DistanceTime =>
			(that canEqual this) &&
				distance == that.distance &&
				time == that.time
		case _ => false
	}
}

case class Link(waitTime: Duration = new Duration(0), travelDistanceTime: DistanceTime = new DistanceTime()) {
	def +(operand: Link): Link = {
		new Link(this.waitTime.plus(operand.waitTime), this.travelDistanceTime + operand.travelDistanceTime)
	}

	def elapsedTime: Duration = travelDistanceTime.time.plus(waitTime)
}

//case class Location(x: BigDecimal = BigDecimal(0), y: BigDecimal = BigDecimal(0), postcode: String = "", id: Option[Int] = None)

class Point(val x: BigDecimal, val y: BigDecimal, val address: String) {
	def +(operand: Point): Point = {
		new Point(this.x + operand.x, this.y + operand.y, "")
	}
	def /(operand: BigDecimal): Point = {
		new Point(this.x / operand, this.y / operand, this.address)
	}
}

case class Depot(name: String, override val x: BigDecimal,  override val y: BigDecimal, override val address: String, userId: Int, id: Option[Int] = None) extends Point(x, y, address)

case class Stop(name: String, override val x: BigDecimal, override val y: BigDecimal, override val address: String, startTime: LocalTime, endTime: LocalTime, maxWeight: BigDecimal, specialCodes: List[String], userId: Int, id: Option[Int] = None) extends Point(x, y, address)

case class LocationMatrix(stops: List[Stop], depots: List[Depot]) extends LatLongTimeAndDistCalc {

	private val distancesAndTimes: Map[Point, Map[Point, DistanceTime]] = {
		(stops ++ depots).map {
			stop1: Point => {
				stop1 -> stops.map {
					stop2: Point =>
						stop2 -> getDistanceTime(stop1, stop2)
				}.toMap[Point, DistanceTime]
			}
		}.toMap
	}

	def findFurthestStop(point: Point): Point  = distancesAndTimes(point).toList.filter(_._1.isInstanceOf[Stop]).sortBy(_._2.distance).last._1

	def distanceTimeBetween(stop1: Point, stop2: Point): DistanceTime = distancesAndTimes(stop1)(stop2)

}



trait LatLongTimeAndDistCalc extends TimeAndDistCalc {

	override def getMetresDistance(location1: Point, location2: Point): BigDecimal = {
		val R = 6371000 // m
		var lat1 = location1.y.toDouble
		var lat2 = location2.y.toDouble
		val lon1 = location1.x.toDouble
		val lon2 = location2.x.toDouble
		val dLat = scala.math.toRadians(lat2 - lat1)
		val dLon = scala.math.toRadians(lon2 - lon1)
		lat1 = scala.math.toRadians(lat1)
		lat2 = scala.math.toRadians(lat2)

		var a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
			Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2)
		var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
		BigDecimal(R * c)//.setScale(2, RoundingMode.HALF_UP)
	}

	override def getDuration(stop1: Point, stop2: Point): Duration = {
		// 50 is 20 m/s which is 72kmph
		new Duration((getMetresDistance(stop1, stop2) * 50).toLong)
	}

}

trait SimpleTimeAndDistCalc extends TimeAndDistCalc {

	override def getMetresDistance(location1: Point, location2: Point): BigDecimal = {
		val xdiff = location1.x.doubleValue() - location2.x.doubleValue()
		val ydiff = location1.y.doubleValue() - location2.y.doubleValue()
		val ans = math.hypot(xdiff, ydiff)
		BigDecimal(ans)
	}

	override def getDuration(location1: Point, location2: Point): Duration = {
		// 10=m/s = 36kmph
		new Duration ((getMetresDistance(location1, location2) * 100).toLong)
	}
}

trait TimeAndDistCalc {

	def getMetresDistance(location1: Point, location2: Point): BigDecimal

	def getDuration(location1: Point, location2: Point): Duration

	def getDistanceTime(stop1: Point, stop2: Point): DistanceTime = {
		new DistanceTime(getMetresDistance(stop1, stop2), getDuration(stop1, stop2))
	}

}

trait Mean {
	def getMean(locations: List[Point]): Option[Point] = {
		if (locations.size > 0)
			Some (locations.foldLeft(new Point(0, 0, "")) { (location1: Point, location2: Point) =>
				new Point(location1.x + location2.x, location1.y + location2.y, "")
			} / locations.size)
		else None
	}
}