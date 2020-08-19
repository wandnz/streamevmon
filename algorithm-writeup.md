# Idea

Instead of backfiltering as we go, why not keep track of all the redundant
detectors while retaining the same count and stopping condition as before. Once
the stop is reached, we can go through the list and find the biggest detectors
which are not redundant with each other. Optimising this problem is likely very
computationally expensive, so we could take a shortcut as follows:

- Sort the list of all detectors (redundant or not) in decreasing order of radius
- Iterate over the list in that order
- For each detector, remove all smaller detectors which are made redundant
  by the large detector

# Steps for data preparation

- Trained using exclusively normal data.
- Data can have any number of dimensions.
- All dimensions should have some variation to them, so feature selection should
  be performed in advance.
- Data should be exclusively numeric.
- Data can be normalised such that each dimension has a range of 0-1, or
  can be provided in a raw state, allowing the algorithm to normalise it.

# Parameters

  - `outer_radius`: A non-negative decimal value. This value represents the
    thickness of a border around the known bounds of normal measurements in
    which detectors will be generated.
    
  - `redundancy_proportion`: A decimal value between 0 (exclusive) and 1
    (inclusive). This value influences the density of the resulting detectors
    by controlling the size of the area that each detector can mark other
    detectors as redundant within.
    
  - `redundancy_count_threshold`: A non-negative decimal value. This value
    influences the density of the resulting detectors by allowing for more
    detectors to be generated before stopping.
    
  - `backfilter`: A boolean value. If true, backfiltering is applied, which
    removes additional redundant detectors.

# Detector generation

## Main algorithm

During training, detectors are generated as follows:

1. If this is the first time through the loop, continue to step 2. Otherwise,
  divide the count of redundant detectors by the number of existing detectors.
  If the resulting value is greater than `redundancy_count_threshold`, stop.

2. Choose a random point in the range \[-`outer_radius`, 1 + `outer_radius`] as 
  the centre of the new detector.

3. Check the list of existing detectors to see if the new detector is made
  redundant by any of them.
  - If the new detector is deemed redundant, increase our count of redundant
    detectors by 1, and go to step 1. Otherwise, continue.

4. Find the nearest normal measurement in the training dataset. Record its 
  location and distance from the new centre.
  
5. Determine the radius of the new detector. This is simply the distance from
  the centre to the nearest normal measurement. Detectors have the same radius
  in all dimensions.
  
6. If backfiltering is disabled, continue to step 7.
  - Otherwise, check the list of existing detectors to see if the new detector
    makes any of them redundant. Note that this is the opposite of the check
    in step 3.
  - If any existing detectors are redundant, remove them from the list.

7. Add the new detector to the list of existing detectors. Continue to step 1.

## Backfiltering algorithm

Detectors can be made redundant by other detectors. The algorithm which
determines this is described by the two following conditionals. If both of them
are true, the new detector is made redundant by the existing detector.

- The distance between the centre of the new detector and the centre of the
  existing detector is less than the radius of the existing detector multiplied
  by `redundancy_proportion`.
  
- The distance between the centre of the new detector and the normal measurement
  which is closest to the existing detector is less than the radius of the
  existing detector.
  
These two conditions create a 'redundancy-judgement zone' which is the shape
of the intersection between two circles. One circle is centred on the existing
detector, with a radius proportional to `redundancy_proportion`. The other is
centred on the existing detector's nearest normal measurement, with the full
radius of the existing detector.

# Testing

Testing requires labelled data which contains a mix of normal measurements and
abnormal measurements. The detection method is to simply check if the
hypersphere of any detector contains each measurement individually. If the
point representing the measurement is contained, it is judged as an abnormal
measurement.

Since the detector radius is determined in a way that prevents detectors from
containing any normal measurement contained in the training set, any measurement
which is observed to be contained in a detector is considered abnormal.

# Improvements

The runtime of the algorithm could be improved in several ways.

- Currently, finding the normal measurement which is closest to a particular
  point is done by brute force, finding the distance to every measurement and
  taking the smallest. This could potentially be improved by storing the data
  in a more search-efficient data structure.
- The same applies to testing, as each measurement is compared with each
  detector until a detector is found which contains it. It could be possible to
  test measurements against a smaller subset of detectors with a more efficient
  search algorithm.
- The algorithm could be effectively multithreaded. Detector generation could
  be performed in parallel, but care would need to be taken when checking
  redundancy, as newly generated detectors could make each other redundant. 
  - One possible approach would be for each thread to generate detectors in a
    separate area, then merge the results later. This could help alleviate the
    runtime complexity of finding the nearest measurement, but it would likely
    require a more complex stopping condition and merging algorithm.

