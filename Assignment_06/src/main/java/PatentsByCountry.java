/**
 * Created by nsantos01 on 3/11/15.
 * Assignment 06 | Problem 01
 * List patents granted to each country.
 */

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class PatentsByCountry extends Configured implements Tool {

    public static class MapClass extends MapReduceBase implements Mapper<Text, Text, Text, Text> {

        public void map(Text key, Text value, OutputCollector<Text, Text> output, Reporter reporter)
                throws IOException {
            // Decided against using a CSV library for performance reasons.
            String[] countries = value.toString().replace("\"", "").split(",");
            // If state is empty (i.e. non-US patent), use country column...
            if (countries[4].isEmpty())
                value.set(countries[3]);
            // otherwise use state column.
            else {
                value.set(countries[4]);
            }

            // Country as key and patent as value.
            output.collect(value, key);
        }
    }

    public static class Reduce extends MapReduceBase implements Reducer<Text, Text, Text, Set<String>> {

        public void reduce(Text key, Iterator<Text> values, OutputCollector<Text, Set<String>> output,
                           Reporter reporter) throws IOException {
            // Using Set will automatically remove duplicates.
            Set<String> patents = new HashSet<String>();
            while (values.hasNext()) {
                patents.add(values.next().toString());
            }
            output.collect(key, patents);
        }
    }

    public int run(String[] args) throws Exception {
        Configuration conf = getConf();
        JobConf job = new JobConf(conf, PatentsByCountry.class);
        Path in = new Path(args[0]);
        Path out = new Path(args[1]);
        FileInputFormat.setInputPaths(job, in);
        FileOutputFormat.setOutputPath(job, out);

        job.setJobName("PatentsByCountry");
        job.setMapperClass(MapClass.class);
        job.setReducerClass(Reduce.class);
        job.setInputFormat(KeyValueTextInputFormat.class);
        job.setOutputFormat(TextOutputFormat.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        job.set("key.value.separator.in.input.line", ",");
        JobClient.runJob(job);
        return 0;
    }
    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new Configuration(), new PatentsByCountry(), args);
        System.exit(res);
    }

}
