
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by nsantos on 3/12/15.
 * Assignment 06 | Problem 06
 * Riemann sum implementation. Ideally, we should have a custom InputFormat that outputs individual elements
 * of the sum.
 */
public class RiemannSum {
    public static class MyMapper extends Mapper<LongWritable, Text, Text, DoubleWritable> {
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String x[] = value.toString().split(",");

            double x1 = Double.parseDouble(x[0]);
            double x2 = Double.parseDouble(x[1]);

            context.write(new Text("Result:"), new DoubleWritable((1/x1)*(x2 - x1)));
        }
    }

    public static class MyReducer extends Reducer<Text, DoubleWritable, Text, DoubleWritable> {
        public void reduce(Text key, Iterable<DoubleWritable> values, Context context)
                throws IOException, InterruptedException {
            double sum = 0;
            for (DoubleWritable value : values) {
                sum += value.get();
            }

            context.write(key, new DoubleWritable(sum));
        }
    }

    private static void genX(int a, int b, long N) throws IOException {
        File f = new File("input/riemann/numbers.txt");

        if (!f.exists()) {
            System.out.println("Creating input file...");

            f.getParentFile().mkdirs();

            FileWriter writer = new FileWriter(f);

            double step = (double)(b - a)/N;
            for (double i = a; i < b - step; i += step) {
                writer.write(String.format("%f,%f\n", i, i + step));
            }

            writer.close();
        }
    }

    public static void main(String[] args) throws Exception {
        // Create numbers file.
        genX(1, 10, 10000);

        Job job = Job.getInstance(new Configuration());

        job.setJarByClass(RiemannSum.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(DoubleWritable.class);
        job.setMapperClass(MyMapper.class);
        job.setReducerClass(MyReducer.class);
        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        FileInputFormat.setInputPaths(job, new Path("input/riemann/numbers.txt"));
        FileOutputFormat.setOutputPath(job, new Path(args[0]));

        boolean status = job.waitForCompletion(true);
        if (status) {
            System.exit(0);
        } else {
            System.exit(1);
        }
    }
}
