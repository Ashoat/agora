import scala.collection.mutable
import scala.slick.driver.PostgresDriver.simple._
import scala.util.continuations._
import CoroutineLoader._

/**
 * As object as represented in the data model. It has the following parts:
 * - ID that globally uniquely identifies this object
 * - Type ID that identifies another object representing this object's schema
 * - Update timestamp that identifies the last time this object was modified
 * - Key-value store that contains data about this object
 * - List of edges to other objects
 * The edges directed from this object are stored separately from the reset of
 * this data. Sometimes we want to fetch just the object, and sometimes we just
 * want the edges. For this reason all values in this object other than the ID
 * are represented as Options.
 */
case class DataObject private (
  id: Long,
  object_type: Option[Long],
  update_time: Option[Int],
  fields: Option[Map[String, String]],
  edges: Option[mutable.Map[Long, Long]]
)

object DataObject {

  private val queued: mutable.LinkedHashSet[Long]
    = mutable.LinkedHashSet[Long]()
  private val objectCache: mutable.Map[Long, DataObject] 
    = mutable.Map[Long, DataObject]()
  private val rowCache: mutable.Map[Long, DataObject] 
    = mutable.Map[Long, DataObject]()

  /**
   * Define a private case class to represent a raw row for a DataObject.
   */
  private case class RawRow(
    id: Long,
    object_type: Long,
    update_time: java.sql.Timestamp,
    fields: String
  ) { 
  
    /**
     * Given a schema, hydrate this row into a DataObject.
     */
    def toDataObject(schema: DataObject) {
      DataObject(
        id,
        Some(object_type),
        Some(update_time.getTime()),
        Some(/* json_decode(uncompress(fields)) */)
      )
    }

  }

  /**
   * Fetch an object from the database.
   */
  def get(id: Long) = {
    // Is it cached? If so, return immediately.
    // If not, reset block to get it...
      // Come back; we have the RawRow now. (how?)
      // Is the schema cached? If so, return immediately.
      // If not, reset block to get it...
        // Finally return.
  }
      

  
    // Is it already cached?
    if (cached.isDefinedAt(id)) {
      Result(cached(id))
    } else reset {
      // Store the ID we want to fetch.
      queued += id
      // Come back later...
      shift { fetch[DataObject]() }
      // We should have the ID fetched now.
      Result(cached(id))
    }
  }

  /**
   * Fetch an object and its schema from the database.
   */
  def get(id: Long, object_type: Long) = {
    // Is it cached? If so, return immediately.
    // If not, reset block to get it...
      // If not, do we have the schema?
        // Fetch the object
          // Come back; we have the RawRow now. (how?)
          // Set up and return object DataObject
        // Fetch the schema and the object
          // Come back; we have the RawRows now. (how?)
          // Set up schema DataObject

    // Is it already cached?
    if (cached.isDefinedAt(id)) {
      Result(cached(id))
    } else reset {
      // Store the IDs we want to fetch. Store schema first
      if (!cached.isDefinedAt(object_type)) {
        queued += object_type
      }
      queued += id
      // Come back later...
      shift { fetch[DataObject]() }
      // We should have the ID fetched now.
      Result(cached(id))
    }
  }

  /**
   * Given a RawRow fetch its schema and hydate it into a DataObject.
   */
  private def processRawRow(row: RawRow) = {
    // Run this part without a reset block. All reset blocks require a
    // corresponding shift block and shift will add an unnecessary ExecState
    // step to this function.
    if (cached.isDefinedAt(row.object_type)) {
      Result(row.toDataObject(cached(row.object_type)))
    } else reset {
      val fetched: Seq[Any] = shift { fetch[DataObject](get(object_type)) }
      val Seq(schema: DataObject) = fetched
      Result(row.toDataObject(schema))
    }
  }

  /**
   * Given a raw row from the database construct a DataObject.
   */
  def fromRow(id: Long, fields: String) = {
  }

  /**
   * Given an DataObject construct a raw database row.
   */
  def toRow(obj: DataObject) {
  }

  /**
   * Actually execute a database query.
   * This gets called by process() between each coroutine step.
   */
  def query() = {
    // TODO magic SQL
    cached ++= queued.map(id => 
      (id, new DataObject(
        id,
        None,
        None,
        Some(Map[String, String]("test" -> "5")),
        None
      ))
    )
    queued.clear()
  }

}
