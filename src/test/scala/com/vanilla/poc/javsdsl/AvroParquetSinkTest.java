package com.vanilla.poc.javsdsl;

import akka.Done;
import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.google.common.collect.Lists;
import com.vanilla.poc.javadsl.AvroParquetSink;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.avro.AvroReadSupport;
import org.apache.parquet.hadoop.ParquetReader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

import static junit.framework.TestCase.assertEquals;

public class AvroParquetSinkTest {

    private ActorSystem system;
    private Materializer materializer;

    private String folder = "javaTestFolder";

    private final String file = "./"+folder+"/test.parquet";

    private final Schema schema =new Schema.Parser().parse("{\"type\":\"record\",\"name\":\"Document\",\"fields\":[{\"name\":\"id\",\"type\":\"string\"},{\"name\":\"body\",\"type\":\"string\"}]}");

    private final Configuration conf = new Configuration();

    private final List<GenericRecord> records = new ArrayList<>();

    @Before
    public void setup() {
        system = ActorSystem.create();
        materializer = ActorMaterializer.create(system);
        conf.setBoolean(AvroReadSupport.AVRO_COMPATIBILITY, true);
        records.add( new GenericRecordBuilder(schema)
                .set("id","1").set("body", "body11").build());
        records.add( new GenericRecordBuilder(schema)
                .set("id","2").set("body", "body12").build());
        records.add( new GenericRecordBuilder(schema).set("id","3").set("body", "body13").build());

    }


    @Test
    public void createNewParquetFile() throws InterruptedException, IOException {

        Sink<GenericRecord, CompletionStage<Done>> sink = AvroParquetSink.create(file, schema, conf);

       Source.from(records).runWith(sink, materializer);

        Thread.sleep(1000);

         assertEquals (records.size(),checkResponse());
         
         

    }

    private int checkResponse() throws IOException {

        Path dataFile = new Path(file + ".parquet");
        ParquetReader<GenericRecord> reader = AvroParquetReader
                .<GenericRecord>builder(dataFile)
                .disableCompatibility()
                .build();
        List<GenericRecord> expectedRecords = Lists.newArrayList();
        GenericRecord rec;
        while ((rec = reader.read()) != null) {
            expectedRecords.add(rec);
        }
        reader.close();
        return expectedRecords.size();

    }

    @After

    public void tearDown()  {
        system = null;
        materializer = null;
        File index = new File(folder);
        String[]entries = index.list();
        if (entries != null) {
            for (String s : entries) {
                File currentFile = new File(index.getPath(), s);
                currentFile.delete();
            }
        }
        index.delete();
    }
}
