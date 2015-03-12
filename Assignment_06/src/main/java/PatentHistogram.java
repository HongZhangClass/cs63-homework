import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;
import java.util.Iterator;

/**
 * Created by nsantos on 3/11/15.
 * Assignment 06 | Problem 02
 * List patents granted to each country.
 */
public class PatentHistogram extends Configured implements Tool {

    public static class MapClass extends MapReduceBase implements Mapper<Text, Text, Text, IntWritable> {
        private IntWritable patentCount = new IntWritable();

        public void map(Text key, Text value, OutputCollector<Text, IntWritable> output, Reporter reporter)
                throws IOException {
            // This split will leave brackets and commas, but that's fine since we only care about the length.
            patentCount.set(value.toString().split(",").length);
            output.collect(key, patentCount);
        }
    }

    public static class Reduce extends MapReduceBase
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

    public int run(String[] args) throws Exception {
        Configuration conf = getConf();
        JobConf job = new JobConf(conf, PatentHistogram.class);
        Path in = new Path(args[0]);
        Path out = new Path(args[1]);
        FileInputFormat.setInputPaths(job, in);
        FileOutputFormat.setOutputPath(job, out);

        job.setJobName("PatentHistogram");
        job.setMapperClass(MapClass.class);
        job.setReducerClass(Reduce.class);

        job.setInputFormat(KeyValueTextInputFormat.class);
        job.setOutputFormat(TextOutputFormat.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        JobClient.runJob(job);

        return 0;
    }

    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new Configuration(), new PatentHistogram(), args);

        System.exit(res);
    }
}
