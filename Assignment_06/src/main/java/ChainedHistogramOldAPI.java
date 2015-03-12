import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by nsantos on 3/11/15.
 * Assignment 06 | Problem 04
 * Chained jobs to group and count patents per country of origin. This version uses the old API.
 */
public class ChainedHistogramOldAPI extends Configured implements Tool {
    public static class MapClass1 extends MapReduceBase implements Mapper<Text, Text, Text, Text> {
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

    public static class Reduce1 extends MapReduceBase implements Reducer<Text, Text, Text, Set<String>> {
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

    public static class MapClass2 extends MapReduceBase implements Mapper<Text, Text, Text, IntWritable> {
        private IntWritable patentCount = new IntWritable();

        public void map(Text key, Text value, OutputCollector<Text, IntWritable> output, Reporter reporter)
                throws IOException {
            // This split will leave brackets and commas, but that's fine since we only care about the length.
            patentCount.set(value.toString().split(",").length);
            output.collect(key, patentCount);
        }
    }

    public static class Reduce2 extends MapReduceBase
            implements Reducer<Text, IntWritable, Text, IntWritable> {
        public void reduce(Text key, Iterator<IntWritable> values,
                           OutputCollector<Text, IntWritable> output, Reporter reporter) throws IOException {
            // This should only run once, since mapper returns full count. But why change something that is working...
            int count = 0;
            while (values.hasNext()) {
                count += values.next().get();
            }
            output.collect(key, new IntWritable(count));
        }
    }

    private JobConf createJob1(Configuration conf, Path in, Path out) {
        JobConf job = new JobConf(conf, ChainedHistogramOldAPI.class);
        FileInputFormat.setInputPaths(job, in);
        FileOutputFormat.setOutputPath(job, out);

        job.setJobName("job1");
        job.setMapperClass(MapClass1.class);
        job.setReducerClass(Reduce1.class);
        job.setInputFormat(KeyValueTextInputFormat.class);
        job.setOutputFormat(TextOutputFormat.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        job.set("key.value.separator.in.input.line", ",");

        return job;
    }

    private JobConf createJob2(Configuration conf, Path in, Path out) {
        JobConf job = new JobConf(conf, ChainedHistogramOldAPI.class);
        FileInputFormat.setInputPaths(job, in);
        FileOutputFormat.setOutputPath(job, out);

        job.setJobName("job2");
        job.setMapperClass(MapClass2.class);
        job.setReducerClass(Reduce2.class);

        job.setInputFormat(KeyValueTextInputFormat.class);
        job.setOutputFormat(TextOutputFormat.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        return job;
    }

    private void cleanup(Path temp, Configuration conf) throws IOException {
        FileSystem fs = temp.getFileSystem(conf);
        fs.delete(temp, true);
    }

    public int run(String[] args) throws Exception {
        Configuration conf = getConf();
        Path in = new Path(args[0]);
        Path out = new Path(args[1]);
        Path temp = new Path("temp");

        JobConf job1 = createJob1(conf, in, temp);
        JobClient.runJob(job1);

        JobConf job2 = createJob2(conf, temp, out);
        JobClient.runJob(job2);

        cleanup(temp, conf);

        return 0;
    }

    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new Configuration(), new ChainedHistogramOldAPI(), args);

        System.exit(res);
    }
}
