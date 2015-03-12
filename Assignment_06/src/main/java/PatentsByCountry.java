import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
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
 * Created by nsantos01 on 3/11/15.
 * Assignment 06 | Problem 01
 * List patents granted to each country.
 */
public class PatentsByCountry {
    public static class MyMapper extends Mapper<Text, Text, Text, Text> {
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

    public static class MyReducer extends Reducer<Text, Text, Text, Set<String>> {
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            // Using Set will automatically remove duplicates.
            Set<String> patents = new HashSet<String>();
            for (Text value : values) {
                patents.add(value.toString());
            }
            context.write(key, patents);
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        conf.set("mapreduce.input.keyvaluelinerecordreader.key.value.separator", ",");

        Job job = Job.getInstance(conf);

        job.setJarByClass(PatentsByCountry.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Set.class);
        job.setMapperClass(MyMapper.class);
        job.setReducerClass(MyReducer.class);
        job.setInputFormatClass(KeyValueTextInputFormat.class);
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
