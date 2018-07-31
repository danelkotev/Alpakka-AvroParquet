package com.vanilla.poc.scaladsl

import java.util.concurrent.TimeUnit

import akka.Done
import akka.stream.scaladsl.Source
import org.apache.avro.generic.{GenericRecord, GenericRecordBuilder}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.parquet.avro.{AvroParquetReader, AvroParquetWriter, AvroReadSupport}
import org.apache.parquet.hadoop.ParquetWriter
import org.specs2.mutable.Specification
import org.specs2.specification.AfterAll

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class AvroParquetSinkSpec extends Specification with AfterAll with AbstractAvroParquet {


  "ParquetSing Sink" should{

    "create new Parquet file" in{
      case class Document(id:String, body:String)


      val docs = List[Document](
        Document("id1", "sdaada"),
        Document("id1", "sdaada"),
        Document("id3", " fvrfecefedfww"))

      val source = Source.fromIterator(() =>docs.iterator)

      val file = folder+"/test.parquet"
      val conf = new Configuration()
      conf.setBoolean(AvroReadSupport.AVRO_COMPATIBILITY, true)
      val writer: ParquetWriter[GenericRecord] = AvroParquetWriter.builder[GenericRecord](new Path(file)).withConf(conf).withSchema(schema).build()
      val sink = AvroParquetSink(writer)

      val result: Future[Done] = source.map { doc =>
        new GenericRecordBuilder(schema)
          .set("id", doc.id).set("body", doc.body).build()
      }.
        runWith(sink)

      Await.result[Done](result,Duration(5,TimeUnit.SECONDS))
      val dataFile =  new org.apache.hadoop.fs.Path(file)

      val reader = AvroParquetReader.builder[GenericRecord](dataFile).withConf(conf).build()

      var r: GenericRecord = reader.read()

      val b: mutable.Builder[GenericRecord, Seq[GenericRecord]] = Seq.newBuilder[GenericRecord]

      while (r != null) {
        b += r
        r = reader.read()
      }
      b.result().length shouldEqual 3
    }

  }

}

