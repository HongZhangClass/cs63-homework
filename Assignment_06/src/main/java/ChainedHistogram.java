import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.KeyValueTextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by nsantos on 3/11/15.
 * Assignment 06 | Problem 03
 * Chained jobs to group and count patents per country of origin.
 */
public class ChainedHistogram {
    public static class MyMapper1 extends Mapper<Text, Text, Text, Text> {
        public void map(Text key, Text value, Context context) throws IOException, InterruptedException {
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
            context.write(value, key);
        }
    }

    public static class MyReducer1 extends Reducer<Text, Text, Text, Set<String>> {
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            // Using Set will automatically remove duplicates.
            Set<String> patents = new HashSet<String>();
            for (Text value : values) {
                patents.add(value.toString());
            }
            context.write(key, patents);
        }
    }

    public static class MyMapper2 extends Mapper<Text, Text, Text, IntWritable> {
        private IntWritable patentCount = new IntWritable();

        public void map(Text key, Text value, Context context) throws IOException, InterruptedException {
            // This split will leave brackets and commas, but that's fine since we only care about the length.
            patentCount.set(value.toString().split(",").length);
            context.write(key, patentCount);
        }
    }

    public static class MyReducer2 extends Reducer<Text, IntWritable, Text, IntWritable> {
        public void reduce(Text key, Iterable<IntWritable> values, Context context)
                throws IOException, InterruptedException {
            // This should only run once, since mapper returns full count. But why change something that is working...
            int count = 0;
            for (IntWritable value : values) {
                count += value.get();
            }
            context.write(key, new IntWritable(count));
        }
    }

    private static Job createJob1(Path in, Path out) throws IOException {
        Configuration conf = new Configuration();
        conf.set("mapreduce.input.keyvaluelinerecordreader.key.value.separator", ",");

        Job job = Job.getInstance(conf);

        job.setJarByClass(PatentsByCountry.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Set.class);
        job.setMapperClass(MyMapper1.class);
        job.setReducerClass(MyReducer1.class);
        job.setInputFormatClass(KeyValueTextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        FileInputFormat.setInputPaths(job, in);
        FileOutputFormat.setOutputPath(job, out);

        return job;
    }

    private static Job createJob2(Path in, Path out) throws IOException {
        Configuration conf = new Configuration();

        Job job = Job.getInstance(conf);

        job.setJarByClass(PatentsByCountry.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);
        job.setMapperClass(MyMapper2.class);
        job.setReducerClass(MyReducer2.class);
        job.setInputFormatClass(KeyValueTextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        FileInputFormat.setInputPaths(job, in);
        FileOutputFormat.setOutputPath(job, out);

        return job;
    }

    private static void cleanup(Path temp) throws IOException {
        Configuration conf = new Configuration();
        FileSystem fs = temp.getFileSystem(conf);
        fs.delete(temp, true);
    }

    public static void main(String[] args) throws Exception {
        Path in = new Path(args[0]);
        Path out = new Path(args[1]);
        Path temp = new Path("temp");

        Job job1 = createJob1(in, temp);
        boolean status1 = job1.waitForCompletion(true);

        Job job2 = createJob2(temp, out);
        boolean status2 = job2.waitForCompletion(true);

        cleanup(temp);

        if (status1 && status2) {
            System.exit(0);
        } else {
            System.exit(1);
        }
    }
}
