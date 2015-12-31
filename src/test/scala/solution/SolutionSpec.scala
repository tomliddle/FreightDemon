package solution

import com.tomliddle.entity.{Stop, Depot}
import com.tomliddle.solution.{TruckAlgorithm, Solution}
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}

class SolutionSpec extends WordSpec with Matchers with BeforeAndAfterEach with TestObjects {


	"solution" when {

		"calculating the cost" should {

			"calculate the correct cost" in {
				solution.cost should equal (truck.cost.get * 3)
			}

			"calculate the correct distance " in {
				solution.distanceTime.distance should equal (truck.distance.get * 3)
			}

			"calculate the correct time " in {
				solution.distanceTime.time should equal (truck.time.get.plus(truck.time.get).plus(truck.time.get))
			}

			"calculate the correct shuffled cost" in {
				val shuffledSolution = solution.shuffle
				val cost = shuffledSolution.cost

				cost should be < (solution.cost)

				cost should equal (truck.shuffle.cost.get * 3)

			}

			"have the right stops" in {
				val shuffledSolution = solution.shuffle

				solution.loadedStops.size should equal (30)
				shuffledSolution.loadedStops.size should equal (30)

			}

			"calculate max swap size" in {
				solution.maxSolutionSwapSize should equal (5)
			}

			"get loaded cities" in {
				solution.loadedStops.size should equal (30)
			}

			"swap 4 " in {
				val stops = List(
					Stop("1", -0.09, 51.55, "234234", startTime, endTime, BigDecimal(1), 1),
					Stop("1", -0.36, 51.58, "234234", startTime, endTime, BigDecimal(1), 1),
					Stop("1", 0.11, 52.25, "234234", startTime, endTime, BigDecimal(1), 1),
					Stop("1", -0.3, 51.47, "234234", startTime, endTime, BigDecimal(1), 1)
				)

				val depot: Depot = Depot("Depot1", 0 ,0 , "", 1, Some(1))
				val solution = Solution("solution", depot, truck.stops, List(truck, truck, truck), lm, 1)
				val shuffledSolution = solution.shuffle

				solution.loadedStops.size should equal (30)
			}

		}
	}
}