import { Metric } from 'web-vitals';

const reportHandler = (metric?: Metric) => {
  console.log(metric);
};

export default reportHandler;
