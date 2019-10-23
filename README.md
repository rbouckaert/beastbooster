# treeops

Tree Operators for [BEAST 2](http://beast2.org)

## How to build

* clone this repo, [BEASTLabs](https://github.com/BEAST2-Dev/BEASTLabs) and [BEAST 2](https://github.com/CompEvol/beast2/)
* run `ant addon` from with the beast2 directory, then BEASTLabs, then treeops
* install treeops package [by hand](http://www.beast2.org/managing-packages/#Install_by_hand)

## How to use the operator

Add operator to XML, like so, but make sure to replace `data` with what is appropriate for your tree

```xml
  <operator id="YuleModelAttach:data" spec="AttachOperator" tree="@Tree.t:data" weight="20.0">
    	<weights spec="AlignmentDistanceProvider" tree="@Tree.t:data" data="@data"/>
  </operator>
```

## Guarantees

None whatsoever.
