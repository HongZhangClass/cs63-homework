import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import java.io.IOException;

/**
 * Created by nsantos on 3/12/15.
 * Assignment 06 | Problem 05
 * List patents granted to each country.
 */

public class PrettyHistogram {
    public static class PrettyMapper extends Mapper<LongWritable, Text, IntWritable, IntWritable> {
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String citationCount[] = value.toString().split("\\s+");

            int bucket = (int)Math.ceil(Integer.parseInt(citationCount[0])/20);

            context.write(new IntWritable(bucket), new IntWritable(Integer.parseInt(citationCount[1])));
        }
    }

    public static class PrettyReducer extends Reducer<IntWritable, IntWritable, Text, Text> {
        public void reduce(IntWritable key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
            // Format buckets.
            int bucketNumber = key.get();
            Text bucketLabel = new Text(String.format("%d-%d", 20*bucketNumber + 1, 20*bucketNumber + 20));

            int total = 0;
            for (IntWritable value : values) {
                total += value.get();
            }

            // Need to cast double from ceil.
            int normCitations = (int)(4 * Math.log10(total + 1));
            Text asterisks = new Text(new String(new char[normCitations]).replace("\0", "*"));

            context.write(bucketLabel, asterisks);
        }
    }

    public static void main(String[] args) throws Exception {
        Job job = Job.getInstance(new Configuration());

        job.setJarByClass(PrettyHistogram.class);
        job.setMapOutputKeyClass(IntWritable.class);
        job.setMapOutputValueClass(IntWritable.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        job.setMapperClass(PrettyMapper.class);
        job.setReducerClass(PrettyReducer.class);
        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        FileInputFormat.setInputPaths(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        boolean status = job.waitForCompletion(true);
        if (status) {
            System.exit(0);
        } else {
            System.exit(1);
        }
    }
}
